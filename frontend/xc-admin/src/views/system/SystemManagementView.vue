<template>
  <section class="system-page">
    <div class="page-heading">
      <div>
        <h1>{{ t('system.title') }}</h1>
        <p>{{ t('system.subtitle') }}</p>
      </div>
      <el-button :loading="refreshing" :icon="Refresh" @click="reloadActive">
        {{ t('common.refresh') }}
      </el-button>
    </div>

    <el-tabs v-model="activeTab" class="system-tabs" @tab-change="handleTabChange">
      <el-tab-pane :label="t('system.tabs.roles')" name="roles">
        <div class="role-layout">
          <el-card shadow="never" class="list-card">
            <template #header>
              <div class="card-header">
                <span>{{ t('system.roleListTitle') }}</span>
                <div class="header-actions">
                  <span>{{ t('system.total', { total: roles.length }) }}</span>
                  <el-button size="small" type="primary" @click="openCreateRoleDialog">
                    {{ t('system.actions.createRole') }}
                  </el-button>
                </div>
              </div>
            </template>
            <el-table
              v-loading="roleLoading"
              :data="roles"
              row-key="id"
              border
              highlight-current-row
              :empty-text="t('system.emptyRoles')"
              @current-change="selectRole"
            >
              <el-table-column :label="t('system.columns.role')" min-width="220">
                <template #default="{ row }">
                  <div class="stack-cell">
                    <strong>{{ row.roleName }}</strong>
                    <span>{{ row.roleCode }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column :label="t('system.columns.permissionCount')" width="130">
                <template #default="{ row }">
                  {{ row.permissions.length }}
                </template>
              </el-table-column>
              <el-table-column prop="description" :label="t('system.columns.description')" min-width="220" />
              <el-table-column :label="t('system.columns.actions')" fixed="right" width="150">
                <template #default="{ row }">
                  <el-button link type="primary" @click.stop="openEditRoleDialog(row)">
                    {{ t('system.actions.edit') }}
                  </el-button>
                  <el-button
                    link
                    type="danger"
                    :disabled="row.roleCode === 'super_admin'"
                    @click.stop="deleteRole(row)"
                  >
                    {{ t('system.actions.delete') }}
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>

          <el-card shadow="never" class="permission-card">
            <template #header>
              <div class="card-header">
                <span>{{ selectedRole ? selectedRole.roleName : t('system.permissionEditorTitle') }}</span>
                <el-button
                  type="primary"
                  :disabled="!selectedRole"
                  :loading="permissionSubmitting"
                  @click="submitRolePermissions"
                >
                  {{ t('system.actions.savePermissions') }}
                </el-button>
              </div>
            </template>
            <el-empty v-if="!selectedRole" :description="t('system.selectRoleHint')" />
            <el-transfer
              v-else
              v-model="selectedPermissionIds"
              filterable
              :data="permissionTransferData"
              :titles="[t('system.availablePermissions'), t('system.selectedPermissions')]"
              class="permission-transfer"
            />
          </el-card>
        </div>
      </el-tab-pane>

      <el-tab-pane :label="t('system.tabs.admins')" name="admins">
        <div class="admin-account-layout">
          <el-card shadow="never" class="list-card">
            <template #header>
              <div class="card-header">
                <span>{{ t('system.adminListTitle') }}</span>
                <div class="header-actions">
                  <span>{{ t('system.total', { total: adminAccountTotal }) }}</span>
                  <el-button size="small" type="primary" @click="openCreateAdminAccountDialog">
                    {{ t('system.actions.createAdmin') }}
                  </el-button>
                </div>
              </div>
            </template>
            <div class="toolbar admin-account-toolbar">
              <el-input
                v-model="adminAccountQuery.keyword"
                clearable
                :placeholder="t('system.adminKeyword')"
                @keyup.enter="loadAdminAccounts"
              />
              <el-select v-model="adminAccountQuery.status" clearable :placeholder="t('system.adminStatusFilter')">
                <el-option label="active" value="active" />
                <el-option label="disabled" value="disabled" />
              </el-select>
              <el-button :icon="Search" type="primary" @click="searchAdminAccounts">{{ t('system.actions.search') }}</el-button>
              <el-button @click="resetAdminAccountFilters">{{ t('system.actions.reset') }}</el-button>
            </div>
            <el-table
              v-loading="adminAccountLoading"
              :data="adminAccounts"
              row-key="id"
              border
              highlight-current-row
              :empty-text="t('system.emptyAdmins')"
              @current-change="selectAdminAccount"
              @sort-change="handleAdminAccountSortChange"
            >
              <el-table-column prop="username" :label="t('system.columns.adminAccount')" min-width="220" sortable="custom">
                <template #default="{ row }">
                  <div class="stack-cell">
                    <strong>{{ row.displayName || row.username }}</strong>
                    <span>{{ row.username }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="status" :label="t('system.columns.status')" width="110" sortable="custom">
                <template #default="{ row }">
                  <el-tag :type="row.status === 'active' ? 'success' : 'info'">
                    {{ t(`system.adminStatuses.${row.status}`) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column :label="t('system.columns.roles')" min-width="220">
                <template #default="{ row }">
                  <div class="tag-list">
                    <el-tag v-for="role in row.roles" :key="role.id" size="small">
                      {{ role.roleName }}
                    </el-tag>
                    <span v-if="row.roles.length === 0">{{ t('common.empty') }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="lastLoginAt" :label="t('system.columns.lastLoginAt')" width="170" sortable="custom">
                <template #default="{ row }">{{ formatDate(row.lastLoginAt) }}</template>
              </el-table-column>
              <el-table-column :label="t('system.columns.actions')" fixed="right" width="160">
                <template #default="{ row }">
                  <el-button link type="primary" @click.stop="openEditAdminAccountDialog(row)">
                    {{ t('system.actions.edit') }}
                  </el-button>
                  <el-button link type="primary" @click.stop="openResetPasswordDialog(row)">
                    {{ t('system.actions.resetPassword') }}
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-pagination
              v-model:current-page="adminAccountQuery.page"
              v-model:page-size="adminAccountQuery.pageSize"
              background
              layout="total, sizes, prev, pager, next"
              :page-sizes="[10, 20, 50, 100]"
              :total="adminAccountTotal"
              @current-change="loadAdminAccounts"
              @size-change="loadAdminAccounts"
            />
          </el-card>

          <el-card shadow="never" class="permission-card">
            <template #header>
              <div class="card-header">
                <span>{{ selectedAdminAccount ? selectedAdminAccount.displayName || selectedAdminAccount.username : t('system.adminRoleEditorTitle') }}</span>
                <el-button
                  type="primary"
                  :disabled="!selectedAdminAccount"
                  :loading="adminRoleSubmitting"
                  @click="submitAdminAccountRoles"
                >
                  {{ t('system.actions.saveRoles') }}
                </el-button>
              </div>
            </template>
            <el-empty v-if="!selectedAdminAccount" :description="t('system.selectAdminHint')" />
            <el-transfer
              v-else
              v-model="selectedAdminRoleIds"
              filterable
              :data="roleTransferData"
              :titles="[t('system.availableRoles'), t('system.selectedRoles')]"
              class="permission-transfer"
            />
          </el-card>
        </div>
      </el-tab-pane>

      <el-tab-pane :label="t('system.tabs.configs')" name="configs">
        <el-card shadow="never" class="list-card matching-stage-card">
          <template #header>
            <div class="card-header stage-card-header">
              <div class="stage-heading">
                <span>{{ t('system.matchingStages.title') }}</span>
                <p>{{ t('system.matchingStages.subtitle') }}</p>
              </div>
              <div class="header-actions">
                <el-button @click="addMatchingStage">{{ t('system.matchingStages.add') }}</el-button>
                <el-button type="primary" :loading="matchingStageSubmitting" @click="saveMatchingStages">
                  {{ t('system.matchingStages.save') }}
                </el-button>
              </div>
            </div>
          </template>
          <el-table
            v-loading="matchingStageLoading"
            :data="matchingStageRows"
            row-key="localId"
            border
            :empty-text="t('system.matchingStages.empty')"
          >
            <el-table-column :label="t('system.matchingStages.code')" min-width="140">
              <template #default="{ row }">
                <el-input v-model="row.code" maxlength="30" />
              </template>
            </el-table-column>
            <el-table-column :label="t('system.matchingStages.labelZh')" min-width="140">
              <template #default="{ row }">
                <el-input v-model="row.labels.zh" maxlength="40" />
              </template>
            </el-table-column>
            <el-table-column :label="t('system.matchingStages.labelEn')" min-width="140">
              <template #default="{ row }">
                <el-input v-model="row.labels.en" maxlength="40" />
              </template>
            </el-table-column>
            <el-table-column :label="t('system.matchingStages.labelRu')" min-width="140">
              <template #default="{ row }">
                <el-input v-model="row.labels.ru" maxlength="40" />
              </template>
            </el-table-column>
            <el-table-column :label="t('system.matchingStages.pairCount')" width="140">
              <template #default="{ row }">
                <el-input-number v-model="row.pairCount" :min="2" :max="30" controls-position="right" />
              </template>
            </el-table-column>
            <el-table-column :label="t('system.matchingStages.cardCount')" width="100">
              <template #default="{ row }">
                {{ row.pairCount * 2 }}
              </template>
            </el-table-column>
            <el-table-column :label="t('system.matchingStages.sortOrder')" width="130">
              <template #default="{ row }">
                <el-input-number v-model="row.sortOrder" :min="0" :max="9999" controls-position="right" />
              </template>
            </el-table-column>
            <el-table-column :label="t('system.matchingStages.enabled')" width="110">
              <template #default="{ row }">
                <el-switch v-model="row.enabled" />
              </template>
            </el-table-column>
            <el-table-column :label="t('system.columns.actions')" fixed="right" width="100">
              <template #default="{ $index }">
                <el-button link type="danger" :disabled="matchingStageRows.length <= 1" @click="removeMatchingStage($index)">
                  {{ t('system.actions.delete') }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-card shadow="never" class="list-card runtime-settings-card">
          <template #header>
            <div class="card-header stage-card-header">
              <div class="stage-heading">
                <span>{{ t('system.runtime.title') }}</span>
                <p>{{ t('system.runtime.subtitle') }}</p>
              </div>
              <el-button type="primary" :loading="runtimeSettingsSubmitting" @click="saveRuntimeSettings">
                {{ t('system.runtime.save') }}
              </el-button>
            </div>
          </template>
          <div v-loading="runtimeSettingsLoading" class="runtime-grid">
            <section class="runtime-section">
              <h3>{{ t('system.runtime.membershipTitle') }}</h3>
              <el-form label-position="top">
                <el-form-item :label="t('system.runtime.trialDays')">
                  <el-input-number v-model="runtimeSettings.membership.trialDays" :min="0" :max="365" controls-position="right" />
                </el-form-item>
              </el-form>
            </section>

            <section class="runtime-section">
              <h3>{{ t('system.runtime.paymentTitle') }}</h3>
              <el-form label-position="top">
                <div class="inline-switches">
                  <el-switch v-model="runtimeSettings.payment.mockEnabled" :active-text="t('system.runtime.mockPayment')" />
                  <el-switch v-model="runtimeSettings.payment.wechatPayEnabled" :active-text="t('system.runtime.wechatPay')" />
                  <el-switch v-model="runtimeSettings.payment.alipayEnabled" :active-text="t('system.runtime.alipay')" />
                </div>
                <el-form-item :label="t('system.runtime.wechatPayUrl')">
                  <el-input v-model="runtimeSettings.payment.wechatPayPaymentUrlPrefix" />
                </el-form-item>
                <el-form-item :label="secretLabel(runtimeSettings.payment.wechatPayNotifySecretConfigured)">
                  <el-input v-model="runtimeSecretForm.wechatPayNotifySecret" show-password :placeholder="t('system.runtime.secretPlaceholder')" />
                  <el-checkbox v-model="runtimeSecretForm.clearWechatPayNotifySecret">{{ t('system.runtime.clearSecret') }}</el-checkbox>
                </el-form-item>
                <el-form-item :label="t('system.runtime.alipayUrl')">
                  <el-input v-model="runtimeSettings.payment.alipayPaymentUrlPrefix" />
                </el-form-item>
                <el-form-item :label="secretLabel(runtimeSettings.payment.alipayNotifySecretConfigured)">
                  <el-input v-model="runtimeSecretForm.alipayNotifySecret" show-password :placeholder="t('system.runtime.secretPlaceholder')" />
                  <el-checkbox v-model="runtimeSecretForm.clearAlipayNotifySecret">{{ t('system.runtime.clearSecret') }}</el-checkbox>
                </el-form-item>
              </el-form>
            </section>

            <section class="runtime-section">
              <h3>{{ t('system.runtime.asrTitle') }}</h3>
              <el-alert class="runtime-alert" type="info" :closable="false" :title="t('system.runtime.restartHint')" />
              <el-form label-position="top">
                <div class="runtime-two-col">
                  <el-form-item :label="t('system.runtime.asrProvider')">
                    <el-select v-model="runtimeSettings.asr.provider">
                      <el-option label="mock" value="mock" />
                      <el-option label="http" value="http" />
                    </el-select>
                  </el-form-item>
                  <el-form-item :label="t('system.runtime.workerEnabled')">
                    <el-switch v-model="runtimeSettings.asr.workerEnabled" />
                  </el-form-item>
                </div>
                <div class="runtime-two-col">
                  <el-form-item :label="t('system.runtime.engineName')">
                    <el-input v-model="runtimeSettings.asr.engineName" />
                  </el-form-item>
                  <el-form-item :label="t('system.runtime.batchSize')">
                    <el-input-number v-model="runtimeSettings.asr.batchSize" :min="1" :max="100" controls-position="right" />
                  </el-form-item>
                </div>
                <el-form-item :label="t('system.runtime.serviceUrl')">
                  <el-input v-model="runtimeSettings.asr.serviceUrl" />
                </el-form-item>
                <div class="runtime-two-col">
                  <el-form-item :label="t('system.runtime.servicePath')">
                    <el-input v-model="runtimeSettings.asr.servicePath" />
                  </el-form-item>
                  <el-form-item :label="t('system.runtime.timeoutMs')">
                    <el-input-number v-model="runtimeSettings.asr.timeoutMs" :min="1000" :max="120000" controls-position="right" />
                  </el-form-item>
                </div>
                <div class="runtime-two-col">
                  <el-form-item :label="t('system.runtime.initialDelayMs')">
                    <el-input-number v-model="runtimeSettings.asr.initialDelayMs" :min="1000" :max="3600000" controls-position="right" />
                  </el-form-item>
                  <el-form-item :label="t('system.runtime.pollDelayMs')">
                    <el-input-number v-model="runtimeSettings.asr.pollDelayMs" :min="1000" :max="3600000" controls-position="right" />
                  </el-form-item>
                </div>
                <el-form-item :label="t('system.runtime.mockRecognizedText')">
                  <el-input v-model="runtimeSettings.asr.mockRecognizedText" type="textarea" :rows="2" />
                </el-form-item>
              </el-form>
            </section>

            <section class="runtime-section">
              <h3>{{ t('system.runtime.uploadTitle') }}</h3>
              <el-alert class="runtime-alert" type="info" :closable="false" :title="t('system.runtime.uploadRestartHint')" />
              <el-form label-position="top">
                <div class="runtime-two-col">
                  <el-form-item :label="t('system.runtime.maxFileSize')">
                    <el-input v-model="runtimeSettings.upload.maxFileSize" />
                  </el-form-item>
                  <el-form-item :label="t('system.runtime.maxRequestSize')">
                    <el-input v-model="runtimeSettings.upload.maxRequestSize" />
                  </el-form-item>
                </div>
                <el-form-item :label="t('system.runtime.audioExtensions')">
                  <el-input v-model="runtimeSettings.upload.audioExtensions" />
                </el-form-item>
                <el-form-item :label="t('system.runtime.imageExtensions')">
                  <el-input v-model="runtimeSettings.upload.imageExtensions" />
                </el-form-item>
                <el-form-item :label="t('system.runtime.videoExtensions')">
                  <el-input v-model="runtimeSettings.upload.videoExtensions" />
                </el-form-item>
              </el-form>
            </section>
          </div>
        </el-card>

        <el-card v-if="configs.length > 0 || configQuery.keyword || configQuery.configGroup" shadow="never" class="list-card">
          <div class="toolbar">
            <el-input
              v-model="configQuery.keyword"
              clearable
              :placeholder="t('system.configKeyword')"
              @keyup.enter="loadConfigs"
            />
            <el-select v-model="configQuery.configGroup" clearable :placeholder="t('system.configGroupFilter')">
              <el-option v-for="group in configGroups" :key="group" :label="t(`system.configGroups.${group}`)" :value="group" />
            </el-select>
            <el-button :icon="Search" type="primary" @click="loadConfigs">{{ t('system.actions.search') }}</el-button>
            <el-button @click="resetConfigFilters">{{ t('system.actions.reset') }}</el-button>
          </div>
          <div class="card-header table-title">
            <span>{{ t('system.configListTitle') }}</span>
            <span>{{ t('system.total', { total: configTotal }) }}</span>
          </div>
          <el-table v-loading="configLoading" :data="configs" row-key="id" border :empty-text="t('system.emptyConfigs')" @sort-change="handleConfigSortChange">
            <el-table-column prop="configKey" :label="t('system.columns.configKey')" min-width="230" sortable="custom" />
            <el-table-column prop="configGroup" :label="t('system.columns.configGroup')" width="130" sortable="custom">
              <template #default="{ row }">
                <el-tag>{{ t(`system.configGroups.${row.configGroup}`) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="t('system.columns.configValue')" min-width="220">
              <template #default="{ row }">
                <span class="code-text">{{ maskedConfigValue(row) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="description" :label="t('system.columns.description')" min-width="220" />
            <el-table-column prop="updatedAt" :label="t('system.columns.updatedAt')" width="180" sortable="custom">
              <template #default="{ row }">{{ formatDate(row.updatedAt) }}</template>
            </el-table-column>
            <el-table-column :label="t('system.columns.actions')" fixed="right" width="110">
              <template #default="{ row }">
                <el-button link type="primary" @click="openConfigDialog(row)">{{ t('system.actions.edit') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-model:current-page="configQuery.page"
            v-model:page-size="configQuery.pageSize"
            background
            layout="total, sizes, prev, pager, next"
            :page-sizes="[10, 20, 50, 100]"
            :total="configTotal"
            @current-change="loadConfigs"
            @size-change="loadConfigs"
          />
        </el-card>
      </el-tab-pane>

      <el-tab-pane :label="t('system.tabs.logs')" name="logs">
        <el-card shadow="never" class="list-card">
          <div class="toolbar log-toolbar">
            <el-input v-model="logQuery.keyword" clearable :placeholder="t('system.logKeyword')" @keyup.enter="loadLogs" />
            <el-input v-model="logQuery.action" clearable :placeholder="t('system.actionFilter')" @keyup.enter="loadLogs" />
            <el-input v-model="logQuery.targetType" clearable :placeholder="t('system.targetTypeFilter')" @keyup.enter="loadLogs" />
            <el-date-picker
              v-model="logTimeRange"
              type="datetimerange"
              :range-separator="t('system.to')"
              :start-placeholder="t('system.createdFrom')"
              :end-placeholder="t('system.createdTo')"
            />
            <el-button :icon="Search" type="primary" @click="loadLogs">{{ t('system.actions.search') }}</el-button>
            <el-button @click="resetLogFilters">{{ t('system.actions.reset') }}</el-button>
          </div>
          <div class="card-header table-title">
            <span>{{ t('system.logListTitle') }}</span>
            <span>{{ t('system.total', { total: logTotal }) }}</span>
          </div>
          <el-table v-loading="logLoading" :data="logs" row-key="id" border :empty-text="t('system.emptyLogs')" @sort-change="handleLogSortChange">
            <el-table-column prop="id" :label="t('system.columns.id')" width="90" sortable="custom" />
            <el-table-column prop="action" :label="t('system.columns.action')" min-width="210" sortable="custom" />
            <el-table-column prop="targetType" :label="t('system.columns.target')" min-width="190" sortable="custom">
              <template #default="{ row }">
                <div class="stack-cell">
                  <strong>{{ row.targetType }}</strong>
                  <span>{{ row.targetId || t('common.empty') }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="adminUserId" :label="t('system.columns.adminUserId')" width="130" sortable="custom" />
            <el-table-column prop="ipAddress" :label="t('system.columns.ipAddress')" min-width="150" sortable="custom" />
            <el-table-column prop="createdAt" :label="t('system.columns.createdAt')" width="180" sortable="custom">
              <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column :label="t('system.columns.actions')" fixed="right" width="110">
              <template #default="{ row }">
                <el-button link type="primary" @click="openLogDrawer(row)">{{ t('system.actions.detail') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-model:current-page="logQuery.page"
            v-model:page-size="logQuery.pageSize"
            background
            layout="total, sizes, prev, pager, next"
            :page-sizes="[10, 20, 50, 100]"
            :total="logTotal"
            @current-change="loadLogs"
            @size-change="loadLogs"
          />
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <el-dialog
      v-model="roleDialogVisible"
      :title="roleEditingId ? t('system.roleDialogEditTitle') : t('system.roleDialogCreateTitle')"
      width="560px"
    >
      <el-form ref="roleFormRef" :model="roleForm" :rules="roleRules" label-position="top">
        <el-form-item :label="t('system.columns.roleCode')" prop="roleCode">
          <el-input v-model="roleForm.roleCode" :disabled="roleEditingId !== null" :placeholder="t('system.roleCodePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('system.columns.roleName')" prop="roleName">
          <el-input v-model="roleForm.roleName" :placeholder="t('system.roleNamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('system.columns.description')" prop="description">
          <el-input v-model="roleForm.description" type="textarea" :rows="3" :placeholder="t('system.descriptionPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">{{ t('system.actions.cancel') }}</el-button>
        <el-button type="primary" :loading="roleSubmitting" @click="submitRole">{{ t('system.actions.submit') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="adminAccountDialogVisible"
      :title="adminAccountEditingId ? t('system.adminDialogEditTitle') : t('system.adminDialogCreateTitle')"
      width="580px"
    >
      <el-form ref="adminAccountFormRef" :model="adminAccountForm" :rules="adminAccountRules" label-position="top">
        <el-form-item :label="t('system.columns.username')" prop="username">
          <el-input v-model="adminAccountForm.username" :disabled="adminAccountEditingId !== null" :placeholder="t('system.usernamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('system.columns.displayName')" prop="displayName">
          <el-input v-model="adminAccountForm.displayName" :placeholder="t('system.displayNamePlaceholder')" />
        </el-form-item>
        <el-form-item v-if="adminAccountEditingId === null" :label="t('common.password')" prop="password">
          <el-input v-model="adminAccountForm.password" show-password :placeholder="t('system.adminPasswordPlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('system.columns.status')" prop="status">
          <el-select v-model="adminAccountForm.status">
            <el-option :label="t('system.adminStatuses.active')" value="active" />
            <el-option :label="t('system.adminStatuses.disabled')" value="disabled" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="adminAccountEditingId === null" :label="t('system.columns.roles')" prop="roleIds">
          <el-select v-model="adminAccountForm.roleIds" multiple clearable :placeholder="t('system.rolesPlaceholder')">
            <el-option v-for="role in roles" :key="role.id" :label="`${role.roleName} (${role.roleCode})`" :value="role.id" />
          </el-select>
          <div class="form-hint">{{ t('system.adminRolesHint') }}</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adminAccountDialogVisible = false">{{ t('system.actions.cancel') }}</el-button>
        <el-button type="primary" :loading="adminAccountSubmitting" @click="submitAdminAccount">{{ t('system.actions.submit') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="passwordDialogVisible" :title="t('system.resetPasswordTitle')" width="480px">
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-position="top">
        <el-form-item :label="t('common.password')" prop="password">
          <el-input v-model="passwordForm.password" show-password :placeholder="t('system.adminPasswordPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">{{ t('system.actions.cancel') }}</el-button>
        <el-button type="primary" :loading="passwordSubmitting" @click="submitPasswordReset">{{ t('system.actions.submit') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="configDialogVisible" :title="t('system.configDialogTitle')" width="620px">
      <el-form ref="configFormRef" :model="configForm" :rules="configRules" label-position="top">
        <el-form-item :label="t('system.columns.configKey')">
          <el-input v-model="configForm.configKey" disabled />
        </el-form-item>
        <el-form-item :label="t('system.columns.configValue')" prop="configValue">
          <el-input v-model="configForm.configValue" type="textarea" :rows="5" :placeholder="t('system.configValuePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('system.columns.description')" prop="description">
          <el-input v-model="configForm.description" type="textarea" :rows="3" :placeholder="t('system.descriptionPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="configDialogVisible = false">{{ t('system.actions.cancel') }}</el-button>
        <el-button type="primary" :loading="configSubmitting" @click="submitConfig">{{ t('system.actions.submit') }}</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="logDrawerVisible" :title="t('system.logDetailTitle')" size="680px">
      <template v-if="selectedLog">
        <el-descriptions :column="2" border>
          <el-descriptions-item :label="t('system.columns.id')">{{ selectedLog.id }}</el-descriptions-item>
          <el-descriptions-item :label="t('system.columns.adminUserId')">{{ selectedLog.adminUserId || t('common.empty') }}</el-descriptions-item>
          <el-descriptions-item :label="t('system.columns.action')">{{ selectedLog.action }}</el-descriptions-item>
          <el-descriptions-item :label="t('system.columns.target')">
            {{ selectedLog.targetType }} / {{ selectedLog.targetId || t('common.empty') }}
          </el-descriptions-item>
          <el-descriptions-item :label="t('system.columns.ipAddress')">{{ selectedLog.ipAddress || t('common.empty') }}</el-descriptions-item>
          <el-descriptions-item :label="t('system.columns.createdAt')">{{ formatDate(selectedLog.createdAt) }}</el-descriptions-item>
        </el-descriptions>
        <pre class="detail-json">{{ formatDetail(selectedLog.detail) }}</pre>
      </template>
    </el-drawer>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import { ApiError } from '@/api/http'
import {
  createAdminAccount,
  createAdminRole,
  deleteAdminRole,
  fetchAdminAccounts,
  fetchAdminOperationLogs,
  fetchAdminPermissions,
  fetchAdminMatchingStages,
  fetchAdminRoles,
  fetchAdminRuntimeSettings,
  fetchAdminSystemConfigs,
  resetAdminAccountPassword,
  updateAdminAccount,
  updateAdminAccountRoles,
  updateAdminMatchingStages,
  updateAdminRole,
  updateAdminRolePermissions,
  updateAdminRuntimeSettings,
  updateAdminSystemConfig
} from '@/api/system'
import type {
  AdminAccount,
  AdminAccountQuery,
  AdminMatchingStage,
  AdminOperationLog,
  AdminPermission,
  AdminRuntimeSettings,
  AdminRole,
  AdminSystemConfig,
  AdminSystemConfigQuery,
  AdminOperationLogQuery,
  SystemConfigGroup
} from '@/types/api'
import { applyTableSort, type TableSortChange } from '@/utils/tableSort'

interface MatchingStageRow extends AdminMatchingStage {
  localId: string
}

const { t } = useI18n()
const activeTab = ref('roles')
const refreshing = ref(false)

const roles = ref<AdminRole[]>([])
const permissions = ref<AdminPermission[]>([])
const selectedRole = ref<AdminRole | null>(null)
const selectedPermissionIds = ref<number[]>([])
const roleLoading = ref(false)
const roleSubmitting = ref(false)
const roleDialogVisible = ref(false)
const roleEditingId = ref<number | null>(null)
const roleFormRef = ref<FormInstance>()
const permissionSubmitting = ref(false)
const roleForm = reactive({
  roleCode: '',
  roleName: '',
  description: ''
})

const adminAccounts = ref<AdminAccount[]>([])
const adminAccountTotal = ref(0)
const adminAccountLoading = ref(false)
const adminAccountSubmitting = ref(false)
const adminRoleSubmitting = ref(false)
const passwordSubmitting = ref(false)
const adminAccountDialogVisible = ref(false)
const passwordDialogVisible = ref(false)
const adminAccountEditingId = ref<number | null>(null)
const passwordTargetAccount = ref<AdminAccount | null>(null)
const adminAccountFormRef = ref<FormInstance>()
const passwordFormRef = ref<FormInstance>()
const selectedAdminAccount = ref<AdminAccount | null>(null)
const selectedAdminRoleIds = ref<number[]>([])
const adminAccountQuery = reactive<AdminAccountQuery>({
  page: 1,
  pageSize: 20,
  keyword: '',
  status: '',
  sortBy: '',
  sortDirection: ''
})
const adminAccountForm = reactive({
  username: '',
  displayName: '',
  password: '',
  status: 'active' as 'active' | 'disabled',
  roleIds: [] as number[]
})
const passwordForm = reactive({
  password: ''
})

const configs = ref<AdminSystemConfig[]>([])
const configTotal = ref(0)
const configLoading = ref(false)
const configSubmitting = ref(false)
const configDialogVisible = ref(false)
const configFormRef = ref<FormInstance>()
const matchingStageRows = ref<MatchingStageRow[]>([])
const matchingStageLoading = ref(false)
const matchingStageSubmitting = ref(false)
const runtimeSettingsLoading = ref(false)
const runtimeSettingsSubmitting = ref(false)
const runtimeSettings = reactive<AdminRuntimeSettings>({
  membership: {
    trialDays: 7
  },
  payment: {
    mockEnabled: false,
    wechatPayEnabled: false,
    wechatPayPaymentUrlPrefix: '',
    wechatPayNotifySecretConfigured: false,
    alipayEnabled: false,
    alipayPaymentUrlPrefix: '',
    alipayNotifySecretConfigured: false
  },
  asr: {
    provider: 'mock',
    workerEnabled: true,
    engineName: 'local-asr',
    serviceUrl: '',
    servicePath: '/recognize',
    timeoutMs: 10000,
    batchSize: 5,
    initialDelayMs: 5000,
    pollDelayMs: 5000,
    mockRecognizedText: ''
  },
  upload: {
    maxFileSize: '20MB',
    maxRequestSize: '20MB',
    audioExtensions: 'mp3,wav,m4a,ogg,aac,webm',
    imageExtensions: 'jpg,jpeg,png,webp,gif',
    videoExtensions: 'mp4,webm,mov'
  }
})
const runtimeSecretForm = reactive({
  wechatPayNotifySecret: '',
  clearWechatPayNotifySecret: false,
  alipayNotifySecret: '',
  clearAlipayNotifySecret: false
})
let nextMatchingStageLocalId = 1
const configGroups: SystemConfigGroup[] = ['payment', 'asr', 'membership', 'upload', 'learning']
const configQuery = reactive<AdminSystemConfigQuery>({
  page: 1,
  pageSize: 20,
  keyword: '',
  configGroup: '',
  sortBy: '',
  sortDirection: ''
})
const configForm = reactive({
  configKey: '',
  configValue: '',
  description: ''
})

const logs = ref<AdminOperationLog[]>([])
const logTotal = ref(0)
const logLoading = ref(false)
const logDrawerVisible = ref(false)
const selectedLog = ref<AdminOperationLog | null>(null)
const logTimeRange = ref<[Date, Date] | null>(null)
const logQuery = reactive<AdminOperationLogQuery>({
  page: 1,
  pageSize: 20,
  keyword: '',
  action: '',
  targetType: '',
  sortBy: '',
  sortDirection: ''
})

const configRules = computed<FormRules>(() => ({
  configValue: [{ max: 2000, message: t('system.validation.configValueTooLong'), trigger: 'blur' }],
  description: [{ max: 500, message: t('system.validation.descriptionTooLong'), trigger: 'blur' }]
}))

const roleRules = computed<FormRules>(() => ({
  roleCode: [
    { required: true, message: t('system.validation.roleCodeRequired'), trigger: 'blur' },
    { pattern: /^[a-z][a-z0-9_:-]*$/, message: t('system.validation.roleCodePattern'), trigger: 'blur' },
    { max: 50, message: t('system.validation.roleCodeTooLong'), trigger: 'blur' }
  ],
  roleName: [
    { required: true, message: t('system.validation.roleNameRequired'), trigger: 'blur' },
    { max: 100, message: t('system.validation.roleNameTooLong'), trigger: 'blur' }
  ],
  description: [{ max: 2000, message: t('system.validation.roleDescriptionTooLong'), trigger: 'blur' }]
}))

const adminAccountRules = computed<FormRules>(() => ({
  username: [
    { required: true, message: t('system.validation.usernameRequired'), trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_.@-]+$/, message: t('system.validation.usernamePattern'), trigger: 'blur' },
    { max: 100, message: t('system.validation.usernameTooLong'), trigger: 'blur' }
  ],
  displayName: [{ max: 100, message: t('system.validation.displayNameTooLong'), trigger: 'blur' }],
  password: [
    { required: adminAccountEditingId.value === null, message: t('system.validation.passwordRequired'), trigger: 'blur' },
    { min: 8, max: 72, message: t('system.validation.passwordLength'), trigger: 'blur' }
  ],
  status: [{ required: true, message: t('system.validation.statusRequired'), trigger: 'change' }],
  roleIds:
    adminAccountEditingId.value === null
      ? [{ type: 'array', required: true, min: 1, message: t('system.validation.rolesRequired'), trigger: 'change' }]
      : []
}))

const passwordRules = computed<FormRules>(() => ({
  password: [
    { required: true, message: t('system.validation.passwordRequired'), trigger: 'blur' },
    { min: 8, max: 72, message: t('system.validation.passwordLength'), trigger: 'blur' }
  ]
}))

const permissionTransferData = computed(() =>
  permissions.value.map(permission => ({
    key: permission.id,
    label: `${permission.permissionName} (${permission.permissionCode})`,
    disabled: false
  }))
)

const roleTransferData = computed(() =>
  roles.value.map(role => ({
    key: role.id,
    label: `${role.roleName} (${role.roleCode})`,
    disabled: false
  }))
)

onMounted(async () => {
  await loadRoles()
})

async function handleTabChange() {
  if (activeTab.value === 'roles' && roles.value.length === 0) {
    await loadRoles()
  }
  if (activeTab.value === 'admins' && adminAccounts.value.length === 0) {
    await loadAdminAccounts()
  }
  if (activeTab.value === 'configs' && configs.value.length === 0 && matchingStageRows.value.length === 0) {
    await loadConfigArea()
  }
  if (activeTab.value === 'logs' && logs.value.length === 0) {
    await loadLogs()
  }
}

async function reloadActive() {
  refreshing.value = true
  try {
    if (activeTab.value === 'roles') {
      await loadRoles()
    } else if (activeTab.value === 'admins') {
      await loadAdminAccounts()
    } else if (activeTab.value === 'configs') {
      await loadConfigArea()
    } else {
      await loadLogs()
    }
  } finally {
    refreshing.value = false
  }
}

async function loadRoles() {
  roleLoading.value = true
  try {
    const [roleResult, permissionResult] = await Promise.all([fetchAdminRoles(), fetchAdminPermissions()])
    roles.value = roleResult
    permissions.value = permissionResult
    const currentRole = selectedRole.value
      ? roles.value.find(role => role.id === selectedRole.value?.id)
      : roles.value[0]
    selectRole(currentRole || null)
  } finally {
    roleLoading.value = false
  }
}

function selectRole(role: AdminRole | null) {
  selectedRole.value = role
  selectedPermissionIds.value = role?.permissions.map(permission => permission.id) || []
}

function openCreateRoleDialog() {
  roleEditingId.value = null
  roleForm.roleCode = ''
  roleForm.roleName = ''
  roleForm.description = ''
  roleDialogVisible.value = true
  roleFormRef.value?.clearValidate()
}

function openEditRoleDialog(role: AdminRole) {
  roleEditingId.value = role.id
  roleForm.roleCode = role.roleCode
  roleForm.roleName = role.roleName
  roleForm.description = role.description || ''
  roleDialogVisible.value = true
  roleFormRef.value?.clearValidate()
}

async function submitRole() {
  const valid = await roleFormRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  roleSubmitting.value = true
  try {
    const payload = {
      roleCode: roleForm.roleCode.trim(),
      roleName: roleForm.roleName.trim(),
      description: roleForm.description.trim() || null
    }
    const saved = roleEditingId.value
      ? await updateAdminRole(roleEditingId.value, payload)
      : await createAdminRole(payload)
    if (roleEditingId.value) {
      roles.value = roles.value.map(role => (role.id === saved.id ? saved : role))
    } else {
      roles.value = [...roles.value, saved].sort((a, b) => a.roleCode.localeCompare(b.roleCode) || a.id - b.id)
    }
    selectRole(saved)
    roleDialogVisible.value = false
    ElMessage.success(t('system.saved'))
  } finally {
    roleSubmitting.value = false
  }
}

async function submitRolePermissions() {
  if (!selectedRole.value) {
    return
  }
  permissionSubmitting.value = true
  try {
    const updated = await updateAdminRolePermissions(selectedRole.value.id, {
      permissionIds: selectedPermissionIds.value
    })
    roles.value = roles.value.map(role => (role.id === updated.id ? updated : role))
    selectRole(updated)
    ElMessage.success(t('system.saved'))
  } finally {
    permissionSubmitting.value = false
  }
}

async function deleteRole(role: AdminRole) {
  if (role.roleCode === 'super_admin') {
    ElMessage.warning(t('system.validation.superAdminRoleProtected'))
    return
  }
  try {
    await ElMessageBox.confirm(t('system.roleDeleteConfirm', { role: role.roleName }), t('system.roleDeleteTitle'), {
      cancelButtonText: t('system.actions.cancel'),
      confirmButtonText: t('system.actions.delete'),
      type: 'warning'
    })
    await deleteAdminRole(role.id)
    roles.value = roles.value.filter(item => item.id !== role.id)
    if (selectedRole.value?.id === role.id) {
      selectRole(roles.value[0] || null)
    }
    ElMessage.success(t('system.deleted'))
  } catch (error) {
    if (isMessageBoxCancel(error)) {
      return
    }
    showSystemError(error)
  }
}

async function ensureRolesLoaded() {
  if (roles.value.length === 0) {
    roles.value = await fetchAdminRoles()
  }
}

async function loadAdminAccounts() {
  adminAccountLoading.value = true
  try {
    await ensureRolesLoaded()
    const result = await fetchAdminAccounts(adminAccountQuery)
    adminAccounts.value = result.records
    adminAccountTotal.value = result.total
    const currentAccount = selectedAdminAccount.value
      ? adminAccounts.value.find(account => account.id === selectedAdminAccount.value?.id)
      : adminAccounts.value[0]
    selectAdminAccount(currentAccount || null)
  } finally {
    adminAccountLoading.value = false
  }
}

function searchAdminAccounts() {
  adminAccountQuery.page = 1
  void loadAdminAccounts()
}

function resetAdminAccountFilters() {
  adminAccountQuery.page = 1
  adminAccountQuery.keyword = ''
  adminAccountQuery.status = ''
  void loadAdminAccounts()
}

function handleAdminAccountSortChange(event: TableSortChange) {
  applyTableSort(adminAccountQuery, event)
  void loadAdminAccounts()
}

function selectAdminAccount(account: AdminAccount | null) {
  selectedAdminAccount.value = account
  selectedAdminRoleIds.value = account?.roles.map(role => role.id) || []
}

async function openCreateAdminAccountDialog() {
  try {
    await ensureRolesLoaded()
    adminAccountEditingId.value = null
    adminAccountForm.username = ''
    adminAccountForm.displayName = ''
    adminAccountForm.password = ''
    adminAccountForm.status = 'active'
    adminAccountForm.roleIds = []
    adminAccountDialogVisible.value = true
    await nextTick()
    adminAccountFormRef.value?.clearValidate()
  } catch (error) {
    showSystemError(error)
  }
}

function openEditAdminAccountDialog(account: AdminAccount) {
  selectAdminAccount(account)
  adminAccountEditingId.value = account.id
  adminAccountForm.username = account.username
  adminAccountForm.displayName = account.displayName || ''
  adminAccountForm.password = ''
  adminAccountForm.status = account.status
  adminAccountForm.roleIds = account.roles.map(role => role.id)
  adminAccountDialogVisible.value = true
  adminAccountFormRef.value?.clearValidate()
}

async function submitAdminAccount() {
  const valid = await adminAccountFormRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  adminAccountSubmitting.value = true
  try {
    const saved = adminAccountEditingId.value
      ? await updateAdminAccount(adminAccountEditingId.value, {
          displayName: adminAccountForm.displayName.trim() || null,
          status: adminAccountForm.status
        })
      : await createAdminAccount({
          username: adminAccountForm.username.trim(),
          displayName: adminAccountForm.displayName.trim() || null,
          password: adminAccountForm.password,
          status: adminAccountForm.status,
          roleIds: adminAccountForm.roleIds
        })
    adminAccounts.value = adminAccountEditingId.value
      ? adminAccounts.value.map(account => (account.id === saved.id ? saved : account))
      : [saved, ...adminAccounts.value]
    selectAdminAccount(saved)
    adminAccountDialogVisible.value = false
    ElMessage.success(t('system.saved'))
  } catch (error) {
    showSystemError(error)
  } finally {
    adminAccountSubmitting.value = false
  }
}

async function submitAdminAccountRoles() {
  if (!selectedAdminAccount.value) {
    return
  }
  if (selectedAdminRoleIds.value.length === 0) {
    ElMessage.warning(t('system.validation.rolesRequired'))
    return
  }
  adminRoleSubmitting.value = true
  try {
    const updated = await updateAdminAccountRoles(selectedAdminAccount.value.id, {
      roleIds: selectedAdminRoleIds.value
    })
    adminAccounts.value = adminAccounts.value.map(account => (account.id === updated.id ? updated : account))
    selectAdminAccount(updated)
    ElMessage.success(t('system.saved'))
  } catch (error) {
    showSystemError(error)
  } finally {
    adminRoleSubmitting.value = false
  }
}

function openResetPasswordDialog(account: AdminAccount) {
  passwordTargetAccount.value = account
  passwordForm.password = ''
  passwordDialogVisible.value = true
  passwordFormRef.value?.clearValidate()
}

async function submitPasswordReset() {
  const valid = await passwordFormRef.value?.validate().catch(() => false)
  if (!valid || !passwordTargetAccount.value) {
    return
  }
  passwordSubmitting.value = true
  try {
    const updated = await resetAdminAccountPassword(passwordTargetAccount.value.id, {
      password: passwordForm.password
    })
    adminAccounts.value = adminAccounts.value.map(account => (account.id === updated.id ? updated : account))
    selectAdminAccount(updated)
    passwordDialogVisible.value = false
    ElMessage.success(t('system.saved'))
  } catch (error) {
    showSystemError(error)
  } finally {
    passwordSubmitting.value = false
  }
}

async function loadConfigArea() {
  await Promise.all([loadConfigs(), loadMatchingStages(), loadRuntimeSettings()])
}

async function loadMatchingStages() {
  matchingStageLoading.value = true
  try {
    const stages = await fetchAdminMatchingStages()
    matchingStageRows.value = stages.map(toMatchingStageRow)
  } finally {
    matchingStageLoading.value = false
  }
}

async function loadRuntimeSettings() {
  runtimeSettingsLoading.value = true
  try {
    const settings = await fetchAdminRuntimeSettings()
    Object.assign(runtimeSettings.membership, settings.membership)
    Object.assign(runtimeSettings.payment, settings.payment)
    Object.assign(runtimeSettings.asr, settings.asr)
    Object.assign(runtimeSettings.upload, settings.upload)
    runtimeSecretForm.wechatPayNotifySecret = ''
    runtimeSecretForm.clearWechatPayNotifySecret = false
    runtimeSecretForm.alipayNotifySecret = ''
    runtimeSecretForm.clearAlipayNotifySecret = false
  } finally {
    runtimeSettingsLoading.value = false
  }
}

function toMatchingStageRow(stage: AdminMatchingStage): MatchingStageRow {
  return {
    ...stage,
    labels: {
      zh: stage.labels.zh || '',
      en: stage.labels.en || '',
      ru: stage.labels.ru || ''
    },
    localId: `stage-${nextMatchingStageLocalId++}`
  }
}

function addMatchingStage() {
  const nextIndex = matchingStageRows.value.length + 1
  matchingStageRows.value.push({
    localId: `stage-${nextMatchingStageLocalId++}`,
    code: `stage_${nextIndex}`,
    labels: {
      zh: '',
      en: '',
      ru: ''
    },
    pairCount: 4,
    cardCount: 8,
    enabled: true,
    sortOrder: nextIndex
  })
}

function removeMatchingStage(index: number) {
  matchingStageRows.value.splice(index, 1)
}

function secretLabel(configured: boolean) {
  return configured ? t('system.runtime.secretConfigured') : t('system.runtime.secretNotConfigured')
}

async function saveRuntimeSettings() {
  runtimeSettingsSubmitting.value = true
  try {
    const saved = await updateAdminRuntimeSettings({
      membership: {
        trialDays: runtimeSettings.membership.trialDays
      },
      payment: {
        mockEnabled: runtimeSettings.payment.mockEnabled,
        wechatPayEnabled: runtimeSettings.payment.wechatPayEnabled,
        wechatPayPaymentUrlPrefix: runtimeSettings.payment.wechatPayPaymentUrlPrefix,
        wechatPayNotifySecret: runtimeSecretForm.wechatPayNotifySecret || null,
        clearWechatPayNotifySecret: runtimeSecretForm.clearWechatPayNotifySecret,
        alipayEnabled: runtimeSettings.payment.alipayEnabled,
        alipayPaymentUrlPrefix: runtimeSettings.payment.alipayPaymentUrlPrefix,
        alipayNotifySecret: runtimeSecretForm.alipayNotifySecret || null,
        clearAlipayNotifySecret: runtimeSecretForm.clearAlipayNotifySecret
      },
      asr: { ...runtimeSettings.asr },
      upload: { ...runtimeSettings.upload }
    })
    Object.assign(runtimeSettings.membership, saved.membership)
    Object.assign(runtimeSettings.payment, saved.payment)
    Object.assign(runtimeSettings.asr, saved.asr)
    Object.assign(runtimeSettings.upload, saved.upload)
    runtimeSecretForm.wechatPayNotifySecret = ''
    runtimeSecretForm.clearWechatPayNotifySecret = false
    runtimeSecretForm.alipayNotifySecret = ''
    runtimeSecretForm.clearAlipayNotifySecret = false
    ElMessage.success(t('system.saved'))
    await loadConfigs()
  } catch (error) {
    showSystemError(error)
  } finally {
    runtimeSettingsSubmitting.value = false
  }
}

function validateMatchingStageRows() {
  if (matchingStageRows.value.length === 0) {
    ElMessage.warning(t('system.matchingStages.validationRequired'))
    return false
  }
  const codes = new Set<string>()
  for (const row of matchingStageRows.value) {
    const code = row.code.trim()
    if (!/^[A-Za-z0-9_-]{1,30}$/.test(code)) {
      ElMessage.warning(t('system.matchingStages.validationCode'))
      return false
    }
    if (codes.has(code)) {
      ElMessage.warning(t('system.matchingStages.validationDuplicate'))
      return false
    }
    codes.add(code)
    if (!row.labels.zh.trim()) {
      ElMessage.warning(t('system.matchingStages.validationName'))
      return false
    }
  }
  return true
}

async function saveMatchingStages() {
  if (!validateMatchingStageRows()) {
    return
  }
  matchingStageSubmitting.value = true
  try {
    const saved = await updateAdminMatchingStages({
      stages: matchingStageRows.value.map(row => ({
        code: row.code.trim(),
        labels: {
          zh: row.labels.zh.trim(),
          en: row.labels.en.trim(),
          ru: row.labels.ru.trim()
        },
        pairCount: row.pairCount,
        enabled: row.enabled,
        sortOrder: row.sortOrder
      }))
    })
    matchingStageRows.value = saved.map(toMatchingStageRow)
    ElMessage.success(t('system.saved'))
    await loadConfigs()
  } catch (error) {
    showSystemError(error)
  } finally {
    matchingStageSubmitting.value = false
  }
}

async function loadConfigs() {
  configLoading.value = true
  try {
    const result = await fetchAdminSystemConfigs(configQuery)
    configs.value = result.records
    configTotal.value = result.total
  } finally {
    configLoading.value = false
  }
}

function resetConfigFilters() {
  configQuery.page = 1
  configQuery.keyword = ''
  configQuery.configGroup = ''
  void loadConfigs()
}

function handleConfigSortChange(event: TableSortChange) {
  applyTableSort(configQuery, event)
  void loadConfigs()
}

function openConfigDialog(config: AdminSystemConfig) {
  configForm.configKey = config.configKey
  configForm.configValue = config.configValue || ''
  configForm.description = config.description || ''
  configDialogVisible.value = true
}

async function submitConfig() {
  await configFormRef.value?.validate()
  configSubmitting.value = true
  try {
    await updateAdminSystemConfig(configForm.configKey, {
      configValue: configForm.configValue,
      description: configForm.description
    })
    configDialogVisible.value = false
    ElMessage.success(t('system.saved'))
    await loadConfigs()
  } finally {
    configSubmitting.value = false
  }
}

async function loadLogs() {
  logLoading.value = true
  try {
    const result = await fetchAdminOperationLogs({
      ...logQuery,
      createdFrom: logTimeRange.value?.[0]?.toISOString(),
      createdTo: logTimeRange.value?.[1]?.toISOString()
    })
    logs.value = result.records
    logTotal.value = result.total
  } finally {
    logLoading.value = false
  }
}

function resetLogFilters() {
  logQuery.page = 1
  logQuery.keyword = ''
  logQuery.action = ''
  logQuery.targetType = ''
  logTimeRange.value = null
  void loadLogs()
}

function handleLogSortChange(event: TableSortChange) {
  applyTableSort(logQuery, event)
  void loadLogs()
}

function openLogDrawer(log: AdminOperationLog) {
  selectedLog.value = log
  logDrawerVisible.value = true
}

function maskedConfigValue(config: AdminSystemConfig) {
  if (!config.configValue) {
    return t('common.empty')
  }
  if (config.configGroup === 'payment' || config.configGroup === 'asr') {
    return '••••••'
  }
  return config.configValue
}

function formatDate(value?: string | null) {
  if (!value) {
    return t('common.empty')
  }
  return new Date(value).toLocaleString()
}

function formatDetail(value?: string | null) {
  if (!value) {
    return t('common.empty')
  }
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}

function showSystemError(error: unknown) {
  if (error instanceof ApiError && (error.status === 401 || error.status === 403)) {
    return
  }
  ElMessage.error(error instanceof Error && error.message ? error.message : t('system.requestFailed'))
}

function isMessageBoxCancel(error: unknown) {
  return error === 'cancel' || error === 'close'
}
</script>

<style scoped>
.system-page {
  display: grid;
  gap: 18px;
}

.page-heading {
  align-items: center;
  display: flex;
  justify-content: space-between;
}

.page-heading h1 {
  font-size: 26px;
  line-height: 1.2;
  margin: 0;
}

.page-heading p {
  color: #64748b;
  margin: 8px 0 0;
}

.system-tabs {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  padding: 16px 18px 22px;
}

.system-tabs :deep(.el-tabs__content) {
  padding-top: 14px;
}

.role-layout,
.admin-account-layout {
  align-items: stretch;
  display: grid;
  gap: 18px;
  grid-template-columns: minmax(420px, 1fr) minmax(620px, 1.45fr);
}

.list-card,
.permission-card {
  border-radius: 4px;
  min-width: 0;
}

.matching-stage-card {
  margin-bottom: 16px;
}

.runtime-settings-card {
  margin-bottom: 16px;
}

.stage-card-header {
  align-items: flex-start;
  gap: 18px;
}

.stage-heading {
  display: grid;
  gap: 6px;
}

.stage-heading p {
  color: #64748b;
  font-size: 13px;
  font-weight: 400;
  line-height: 1.5;
  margin: 0;
}

.matching-stage-card :deep(.el-input-number) {
  width: 100%;
}

.runtime-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.runtime-section {
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  padding: 16px;
}

.runtime-section h3 {
  color: #0f172a;
  font-size: 15px;
  line-height: 1.4;
  margin: 0 0 14px;
}

.inline-switches {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 14px 18px;
  margin-bottom: 18px;
}

.runtime-two-col {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.runtime-alert {
  margin-bottom: 14px;
}

.runtime-settings-card :deep(.el-input-number),
.runtime-settings-card :deep(.el-select) {
  width: 100%;
}

.role-layout .list-card,
.role-layout .permission-card,
.admin-account-layout .list-card,
.admin-account-layout .permission-card {
  min-height: 540px;
}

.role-layout .list-card :deep(.el-card__body),
.role-layout .permission-card :deep(.el-card__body),
.admin-account-layout .list-card :deep(.el-card__body),
.admin-account-layout .permission-card :deep(.el-card__body) {
  min-height: 430px;
}

.card-header {
  align-items: center;
  display: flex;
  font-weight: 700;
  justify-content: space-between;
}

.card-header span:last-child {
  color: #64748b;
  font-size: 13px;
  font-weight: 500;
}

.header-actions {
  align-items: center;
  display: flex;
  gap: 10px;
}

.header-actions span {
  color: #64748b;
  font-size: 13px;
  font-weight: 500;
}

.toolbar {
  display: grid;
  gap: 10px;
  grid-template-columns: minmax(220px, 1fr) 180px auto auto;
  margin-bottom: 16px;
}

.log-toolbar {
  grid-template-columns: minmax(280px, 1.5fr) minmax(160px, 0.45fr) minmax(160px, 0.45fr) minmax(360px, 420px) max-content max-content;
}

.toolbar :deep(.el-date-editor),
.toolbar :deep(.el-input),
.toolbar :deep(.el-select) {
  min-width: 0;
  width: 100%;
}

.admin-account-toolbar {
  grid-template-columns: minmax(220px, 1fr) 160px auto auto;
}

.table-title {
  margin-bottom: 12px;
}

.stack-cell {
  display: grid;
  gap: 4px;
}

.stack-cell span {
  color: #64748b;
  font-size: 12px;
}

.tag-list {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.tag-list span {
  color: #64748b;
  font-size: 12px;
}

.code-text {
  color: #334155;
  font-family: "SFMono-Regular", Consolas, "Liberation Mono", monospace;
  font-size: 12px;
}

.form-hint {
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
  margin-top: 6px;
}

.permission-transfer {
  align-items: stretch;
  display: flex;
  gap: 14px;
  width: 100%;
}

.permission-transfer :deep(.el-transfer-panel) {
  flex: 1 1 0;
  min-width: 0;
  width: auto;
}

.permission-transfer :deep(.el-transfer__buttons) {
  align-items: center;
  display: flex;
  flex: 0 0 72px;
  flex-direction: column;
  justify-content: center;
  padding: 0;
}

.permission-transfer :deep(.el-transfer__button) {
  border-radius: 4px;
  margin: 6px 0;
}

.permission-transfer :deep(.el-transfer-panel__body) {
  height: 376px;
}

.permission-transfer :deep(.el-transfer-panel__list.is-filterable) {
  height: 302px;
}

.el-pagination {
  justify-content: flex-end;
  margin-top: 16px;
}

.detail-json {
  background: #0f172a;
  border-radius: 4px;
  color: #dbeafe;
  font-size: 13px;
  line-height: 1.6;
  margin: 16px 0 0;
  max-height: 420px;
  overflow: auto;
  padding: 14px;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 1200px) {
  .role-layout,
  .admin-account-layout,
  .toolbar,
  .log-toolbar,
  .runtime-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .runtime-two-col {
    grid-template-columns: 1fr;
  }

  .permission-transfer {
    display: grid;
  }

  .permission-transfer :deep(.el-transfer__buttons) {
    flex-direction: row;
    min-height: 48px;
  }

  .permission-transfer :deep(.el-transfer__button) {
    margin: 0 6px;
  }
}
</style>
