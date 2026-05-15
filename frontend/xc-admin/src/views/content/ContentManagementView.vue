<template>
  <section class="content-page">
    <div class="page-heading">
      <div>
        <h1>{{ t('content.title') }}</h1>
        <p>{{ t('content.subtitle') }}</p>
      </div>
      <el-button :loading="activeTabLoading" :icon="Refresh" @click="reloadActive">
        {{ t('common.refresh') }}
      </el-button>
    </div>

    <el-tabs v-model="activeTab" class="admin-tabs" @tab-change="handleTabChange">
      <el-tab-pane :label="t('content.tabs.vocabLists')" name="lists">
        <el-card shadow="never" class="filter-card">
          <el-form class="list-filter-form" :model="listQuery" @submit.prevent>
            <el-form-item>
              <el-input
                v-model="listQuery.keyword"
                clearable
                :prefix-icon="Search"
                :placeholder="t('content.listKeyword')"
                @keyup.enter="searchLists"
              />
            </el-form-item>
            <el-form-item>
              <el-select v-model="listQuery.listType" clearable :placeholder="t('content.typeFilter')">
                <el-option v-for="type in vocabTypes" :key="type" :label="t(`content.listTypes.${type}`)" :value="type" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-input v-model="listQuery.level" clearable :placeholder="t('content.levelFilter')" />
            </el-form-item>
            <el-form-item>
              <el-select v-model="listQuery.status" clearable :placeholder="t('content.statusFilter')">
                <el-option :label="t('content.status.all')" :value="''" />
                <el-option :label="t('content.status.active')" value="active" />
                <el-option :label="t('content.status.inactive')" value="inactive" />
              </el-select>
            </el-form-item>
            <el-form-item class="filter-actions">
              <el-button type="primary" :icon="Search" @click="searchLists">{{ t('common.search') }}</el-button>
              <el-button @click="resetListFilters">{{ t('content.reset') }}</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" class="table-card">
          <template #header>
            <div class="card-header">
              <div class="card-title-area">
                <div class="card-title-group">
                  <span>{{ t('content.listTitle') }}</span>
                  <span>{{ t('content.total', { total: listTotal }) }}</span>
                </div>
                <div class="card-actions">
                  <el-button type="primary" plain :icon="Plus" @click="openListDialog()">
                    {{ t('content.actions.createList') }}
                  </el-button>
                  <el-button plain :icon="Upload" @click="openCsvImportDialog('vocab-lists')">
                    {{ t('content.actions.importCsvWithTemplate') }}
                  </el-button>
                </div>
              </div>
              <div v-if="selectedLists.length" class="batch-actions">
                <span>{{ t('content.batchStatus.selected', { count: selectedLists.length }) }}</span>
                <el-button size="small" type="success" plain :loading="batchStatusSubmitting" @click="submitBatchStatus('lists', 'active')">
                  {{ t('content.actions.batchEnable') }}
                </el-button>
                <el-button size="small" type="warning" plain :loading="batchStatusSubmitting" @click="submitBatchStatus('lists', 'inactive')">
                  {{ t('content.actions.batchDisable') }}
                </el-button>
              </div>
            </div>
          </template>
          <el-table
            class="hierarchy-table"
            v-loading="listLoading"
            :data="vocabLists"
            row-key="id"
            border
            :tree-props="treeTableProps"
            :empty-text="t('content.emptyLists')"
            @selection-change="setSelectedLists"
            @sort-change="handleListSortChange"
          >
            <el-table-column type="selection" width="48" />
            <el-table-column prop="name" :label="t('content.columns.vocabList')" min-width="240" sortable="custom">
              <template #default="{ row }">
                <div class="main-cell">
                  <strong>{{ row.name }}</strong>
                  <span>{{ row.description || t('common.empty') }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="id" label="ID" width="84" sortable="custom" />
            <el-table-column prop="listType" :label="t('content.columns.type')" width="140" sortable="custom">
              <template #default="{ row }">
                {{ t(`content.listTypes.${row.listType}`) }}
              </template>
            </el-table-column>
            <el-table-column prop="level" :label="t('content.columns.level')" width="120" sortable="custom" />
            <el-table-column :label="t('content.columns.items')" width="160">
              <template #default="{ row }">
                {{ t('content.itemSummary', { active: row.activeItemCount, inactive: row.inactiveItemCount }) }}
                <span v-if="row.childCount"> · {{ t('content.childSummary', { count: row.childCount }) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="sortOrder" :label="t('content.columns.sortOrder')" width="100" sortable="custom" />
            <el-table-column prop="status" :label="t('content.columns.status')" width="120" sortable="custom">
              <template #default="{ row }">
                <el-tag :type="row.status === 'active' ? 'success' : 'info'">
                  {{ t(`content.status.${row.status}`) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="updatedAt" :label="t('content.columns.updatedAt')" min-width="170" sortable="custom">
              <template #default="{ row }">
                {{ formatDate(row.updatedAt) }}
              </template>
            </el-table-column>
            <el-table-column :label="t('content.columns.actions')" fixed="right" width="210">
              <template #default="{ row }">
                <el-button link type="primary" @click="openListDialog(row)">{{ t('content.actions.edit') }}</el-button>
                <el-button link type="primary" @click="filterItemsByList(row)">{{ t('content.actions.items') }}</el-button>
                <el-button link :type="row.status === 'active' ? 'warning' : 'success'" @click="toggleListStatus(row)">
                  {{ row.status === 'active' ? t('content.actions.disable') : t('content.actions.enable') }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-row">
            <el-pagination
              v-model:current-page="listQuery.page"
              v-model:page-size="listQuery.pageSize"
              :total="listTotal"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleListPageSizeChange"
              @current-change="loadLists"
            />
          </div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane :label="t('content.tabs.vocabItems')" name="items">
        <el-card shadow="never" class="filter-card">
          <el-form class="item-filter-form" :model="itemQuery" @submit.prevent>
            <el-form-item>
              <el-input
                v-model="itemQuery.keyword"
                clearable
                :prefix-icon="Search"
                :placeholder="t('content.itemKeyword')"
                @keyup.enter="searchItems"
              />
            </el-form-item>
            <el-form-item>
              <el-tree-select
                v-model="itemQuery.vocabListId"
                class="full-input content-tree-select"
                clearable
                filterable
                check-strictly
                default-expand-all
                :data="vocabListTreeSelectOptions"
                :render-after-expand="false"
                popper-class="content-tree-select-popper"
                :placeholder="t('content.listFilter')"
                @change="searchItems"
              >
                <template #default="{ data }">
                  <span class="tree-select-node-label" :title="data.label">{{ data.nodeLabel }}</span>
                </template>
              </el-tree-select>
            </el-form-item>
            <el-form-item>
              <el-select v-model="itemQuery.status" clearable :placeholder="t('content.statusFilter')" @change="searchItems">
                <el-option :label="t('content.status.all')" :value="''" />
                <el-option :label="t('content.status.active')" value="active" />
                <el-option :label="t('content.status.inactive')" value="inactive" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-select v-model="itemQuery.hasAudio" clearable :placeholder="t('content.audioFilter')" @change="searchItems">
                <el-option :label="t('content.assetFilters.withAudio')" :value="true" />
                <el-option :label="t('content.assetFilters.missingAudio')" :value="false" />
              </el-select>
            </el-form-item>
            <el-form-item class="filter-actions">
              <el-button type="primary" :icon="Search" @click="searchItems">{{ t('common.search') }}</el-button>
              <el-button @click="resetItemFilters">{{ t('content.reset') }}</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" class="table-card">
          <template #header>
            <div class="card-header">
              <div class="card-title-area">
                <div class="card-title-group">
                  <span>{{ t('content.itemTitle') }}</span>
                  <span>{{ t('content.total', { total: itemTotal }) }}</span>
                </div>
                <div class="card-actions">
                  <el-button type="primary" plain :icon="Plus" :disabled="vocabItemsReadonly" @click="openItemDialog()">
                    {{ t('content.actions.createItem') }}
                  </el-button>
                  <el-button plain :icon="Upload" :disabled="vocabItemsReadonly" @click="openCsvImportDialog('vocab-items')">
                    {{ t('content.actions.importCsvWithTemplate') }}
                  </el-button>
                  <el-button plain :icon="Link" :disabled="vocabItemsReadonly" @click="openBulkBindDialog('itemAudio')">
                    {{ t('content.actions.bulkBindAudio') }}
                  </el-button>
                </div>
              </div>
              <div v-if="selectedItems.length" class="batch-actions">
                <span>{{ t('content.batchStatus.selected', { count: selectedItems.length }) }}</span>
                <el-button size="small" type="primary" plain :loading="batchAssignmentSubmitting" :disabled="selectedItemsReadonly" @click="openBatchAssignmentDialog('items')">
                  {{ t('content.actions.batchUpdateVocabLists') }}
                </el-button>
                <el-button size="small" type="success" plain :loading="batchStatusSubmitting" :disabled="selectedItemsReadonly" @click="submitBatchStatus('items', 'active')">
                  {{ t('content.actions.batchEnable') }}
                </el-button>
                <el-button size="small" type="warning" plain :loading="batchStatusSubmitting" :disabled="selectedItemsReadonly" @click="submitBatchStatus('items', 'inactive')">
                  {{ t('content.actions.batchDisable') }}
                </el-button>
              </div>
            </div>
          </template>
          <el-alert
            v-if="vocabItemsReadonly"
            class="readonly-alert"
            type="warning"
            show-icon
            :closable="false"
            :title="t('content.parentReadonly.vocabItems')"
          />
          <el-table
            v-loading="itemLoading"
            :data="vocabItems"
            row-key="id"
            border
            :empty-text="t('content.emptyItems')"
            @selection-change="setSelectedItems"
            @sort-change="handleItemSortChange"
          >
            <el-table-column type="selection" width="48" />
            <el-table-column prop="id" label="ID" width="84" sortable="custom" />
            <el-table-column prop="hanzi" :label="t('content.columns.word')" min-width="190" sortable="custom">
              <template #default="{ row }">
                <div class="main-cell">
                  <strong>{{ row.hanzi }}</strong>
                  <span>{{ row.pinyin || t('common.empty') }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column :label="t('content.columns.vocabList')" min-width="280">
              <template #default="{ row }">
                <div class="set-path-list">
                  <el-tag
                    v-for="assignment in vocabItemListTagItems(row)"
                    :key="`${row.id}-list-${assignment.id ?? assignment.label}`"
                    class="set-path-tag"
                    :closable="assignment.id !== null"
                    effect="plain"
                    size="small"
                    type="info"
                    @close="removeVocabItemListAssignment(row, assignment.id)"
                  >
                    {{ assignment.label }}
                  </el-tag>
                  <span v-if="!vocabItemListTagItems(row).length" class="muted-text">{{ t('common.empty') }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="meaningEn" :label="t('content.columns.meaningEn')" min-width="180" show-overflow-tooltip sortable="custom" />
            <el-table-column prop="meaningRu" :label="t('content.columns.meaningRu')" min-width="180" show-overflow-tooltip sortable="custom" />
            <el-table-column :label="t('content.columns.audio')" min-width="130">
              <template #default="{ row }">
                <el-link v-if="row.audioUrl" :href="row.audioUrl" target="_blank" type="primary">#{{ row.audioAssetId }}</el-link>
                <span v-else>{{ t('common.empty') }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="sortOrder" :label="t('content.columns.sortOrder')" width="100" sortable="custom" />
            <el-table-column prop="status" :label="t('content.columns.status')" width="120" sortable="custom">
              <template #default="{ row }">
                <el-tag :type="row.status === 'active' ? 'success' : 'info'">
                  {{ t(`content.status.${row.status}`) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="updatedAt" :label="t('content.columns.updatedAt')" min-width="170" sortable="custom">
              <template #default="{ row }">
                {{ formatDate(row.updatedAt) }}
              </template>
            </el-table-column>
            <el-table-column :label="t('content.columns.actions')" fixed="right" width="160">
              <template #default="{ row }">
                <el-button link type="primary" :disabled="isVocabItemReadonly(row)" @click="openItemDialog(row)">{{ t('content.actions.edit') }}</el-button>
                <el-button link :disabled="isVocabItemReadonly(row)" :type="row.status === 'active' ? 'warning' : 'success'" @click="toggleItemStatus(row)">
                  {{ row.status === 'active' ? t('content.actions.disable') : t('content.actions.enable') }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-row">
            <el-pagination
              v-model:current-page="itemQuery.page"
              v-model:page-size="itemQuery.pageSize"
              :total="itemTotal"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleItemPageSizeChange"
              @current-change="loadItems"
            />
          </div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane :label="t('content.tabs.mediaAssets')" name="media">
        <el-card shadow="never" class="filter-card">
          <el-form class="media-filter-form" :model="mediaQuery" @submit.prevent>
            <el-form-item>
              <el-input
                v-model="mediaQuery.keyword"
                clearable
                :prefix-icon="Search"
                :placeholder="t('content.mediaKeyword')"
                @keyup.enter="searchMedia"
              />
            </el-form-item>
            <el-form-item>
              <el-select v-model="mediaQuery.mediaType" clearable :placeholder="t('content.mediaTypeFilter')">
                <el-option :label="t('content.mediaTypes.audio')" value="audio" />
                <el-option :label="t('content.mediaTypes.image')" value="image" />
                <el-option :label="t('content.mediaTypes.video')" value="video" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-select v-model="mediaQuery.language" clearable :placeholder="t('content.languageFilter')">
                <el-option :label="t('content.languages.zh')" value="zh" />
                <el-option :label="t('content.languages.ru')" value="ru" />
                <el-option :label="t('content.languages.en')" value="en" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-select v-model="mediaQuery.status" clearable :placeholder="t('content.statusFilter')">
                <el-option :label="t('content.status.all')" :value="''" />
                <el-option :label="t('content.status.active')" value="active" />
                <el-option :label="t('content.status.inactive')" value="inactive" />
              </el-select>
            </el-form-item>
            <el-form-item class="filter-actions">
              <el-button type="primary" :icon="Search" @click="searchMedia">{{ t('common.search') }}</el-button>
              <el-button @click="resetMediaFilters">{{ t('content.reset') }}</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" class="table-card">
          <template #header>
            <div class="card-header">
              <div class="card-title-area">
                <div class="card-title-group">
                  <span>{{ t('content.mediaTitle') }}</span>
                  <span>{{ t('content.total', { total: mediaTotal }) }}</span>
                </div>
                <div class="card-actions">
                  <el-button type="primary" plain :icon="Plus" @click="openUploadDialog">
                    {{ t('content.actions.uploadMedia') }}
                  </el-button>
                </div>
              </div>
              <div v-if="selectedMediaAssets.length" class="batch-actions">
                <span>{{ t('content.batchStatus.selected', { count: selectedMediaAssets.length }) }}</span>
                <el-button size="small" type="success" plain :loading="batchStatusSubmitting" @click="submitBatchStatus('media', 'active')">
                  {{ t('content.actions.batchEnable') }}
                </el-button>
                <el-button size="small" type="warning" plain :loading="batchStatusSubmitting" @click="submitBatchStatus('media', 'inactive')">
                  {{ t('content.actions.batchDisable') }}
                </el-button>
              </div>
            </div>
          </template>
          <el-table
            v-loading="mediaLoading"
            :data="mediaAssets"
            row-key="id"
            border
            :empty-text="t('content.emptyMedia')"
            @selection-change="setSelectedMediaAssets"
            @sort-change="handleMediaSortChange"
          >
            <el-table-column type="selection" width="48" />
            <el-table-column prop="id" label="ID" width="84" sortable="custom" />
            <el-table-column prop="originalFilename" :label="t('content.columns.media')" min-width="360" sortable="custom">
              <template #default="{ row }">
                <div class="main-cell">
                  <strong>{{ mediaPrimaryLabel(row) }}</strong>
                  <span>{{ formatFileSize(row.fileSize) }}</span>
                  <span>{{ row.url }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="mediaType" :label="t('content.columns.mediaType')" width="120" sortable="custom">
              <template #default="{ row }">
                {{ t(`content.mediaTypes.${row.mediaType}`) }}
              </template>
            </el-table-column>
            <el-table-column prop="language" :label="t('content.columns.language')" width="120" sortable="custom">
              <template #default="{ row }">
                {{ row.language ? t(`content.languages.${row.language}`) : t('common.empty') }}
              </template>
            </el-table-column>
            <el-table-column prop="durationMs" :label="t('content.columns.duration')" width="130" sortable="custom">
              <template #default="{ row }">
                {{ formatDurationMs(row.durationMs) }}
              </template>
            </el-table-column>
            <el-table-column prop="status" :label="t('content.columns.status')" width="110" sortable="custom">
              <template #default="{ row }">
                <el-tag :type="row.status === 'active' ? 'success' : 'info'">
                  {{ t(`content.status.${row.status}`) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" :label="t('content.columns.createdAt')" min-width="170" sortable="custom">
              <template #default="{ row }">
                {{ formatDate(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column :label="t('content.columns.actions')" fixed="right" width="240">
              <template #default="{ row }">
                <div class="media-row-actions">
                  <el-link :href="row.url" target="_blank" type="primary">{{ t('content.actions.open') }}</el-link>
                  <el-button link :type="row.status === 'active' ? 'warning' : 'success'" @click="toggleMediaStatus(row)">
                    {{ row.status === 'active' ? t('content.actions.disable') : t('content.actions.enable') }}
                  </el-button>
                  <el-button link type="danger" :disabled="row.status === 'active'" @click="deleteMediaAsset(row)">
                    {{ t('content.actions.delete') }}
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-row">
            <el-pagination
              v-model:current-page="mediaQuery.page"
              v-model:page-size="mediaQuery.pageSize"
              :total="mediaTotal"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleMediaPageSizeChange"
              @current-change="loadMediaAssets"
            />
          </div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane :label="t('content.tabs.exerciseSets')" name="sets">
        <el-card shadow="never" class="filter-card">
          <el-form class="set-filter-form" :model="setQuery" @submit.prevent>
            <el-form-item>
              <el-input
                v-model="setQuery.keyword"
                clearable
                :prefix-icon="Search"
                :placeholder="t('content.setKeyword')"
                @keyup.enter="searchSets"
              />
            </el-form-item>
            <el-form-item>
              <el-select v-model="setQuery.exerciseType" clearable :placeholder="t('content.exerciseTypeFilter')">
                <el-option v-for="type in exerciseTypes" :key="type" :label="t(`content.exerciseTypes.${type}`)" :value="type" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-input v-model="setQuery.level" clearable :placeholder="t('content.levelFilter')" />
            </el-form-item>
            <el-form-item>
              <el-select v-model="setQuery.status" clearable :placeholder="t('content.statusFilter')">
                <el-option :label="t('content.status.all')" :value="''" />
                <el-option :label="t('content.status.active')" value="active" />
                <el-option :label="t('content.status.inactive')" value="inactive" />
              </el-select>
            </el-form-item>
            <el-form-item class="filter-actions">
              <el-button type="primary" :icon="Search" @click="searchSets">{{ t('common.search') }}</el-button>
              <el-button @click="resetSetFilters">{{ t('content.reset') }}</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" class="table-card">
          <template #header>
            <div class="card-header">
              <div class="card-title-area">
                <div class="card-title-group">
                  <span>{{ t('content.setTitle') }}</span>
                  <span>{{ t('content.total', { total: setTotal }) }}</span>
                </div>
                <div class="card-actions">
                  <el-button type="primary" plain :icon="Plus" @click="openSetDialog()">
                    {{ t('content.actions.createSet') }}
                  </el-button>
                  <el-button plain :icon="Upload" @click="openCsvImportDialog('exercise-sets')">
                    {{ t('content.actions.importCsvWithTemplate') }}
                  </el-button>
                </div>
              </div>
              <div v-if="selectedSets.length" class="batch-actions">
                <span>{{ t('content.batchStatus.selected', { count: selectedSets.length }) }}</span>
                <el-button size="small" type="success" plain :loading="batchStatusSubmitting" @click="submitBatchStatus('sets', 'active')">
                  {{ t('content.actions.batchEnable') }}
                </el-button>
                <el-button size="small" type="warning" plain :loading="batchStatusSubmitting" @click="submitBatchStatus('sets', 'inactive')">
                  {{ t('content.actions.batchDisable') }}
                </el-button>
              </div>
            </div>
          </template>
          <el-table
            class="hierarchy-table"
            v-loading="setLoading"
            :data="exerciseSets"
            row-key="id"
            border
            :tree-props="treeTableProps"
            :empty-text="t('content.emptySets')"
            @selection-change="setSelectedSets"
            @sort-change="handleSetSortChange"
          >
            <el-table-column type="selection" width="48" />
            <el-table-column prop="title" :label="t('content.columns.exerciseSet')" min-width="240" sortable="custom">
              <template #default="{ row }">
                <div class="main-cell">
                  <strong>{{ row.title }}</strong>
                  <span>{{ row.level || t('common.empty') }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="id" label="ID" width="84" sortable="custom" />
            <el-table-column prop="exerciseType" :label="t('content.columns.type')" min-width="160" sortable="custom">
              <template #default="{ row }">
                {{ t(`content.exerciseTypes.${row.exerciseType}`) }}
              </template>
            </el-table-column>
            <el-table-column :label="t('content.columns.exercises')" width="170">
              <template #default="{ row }">
                {{ t('content.exerciseSummary', { active: row.activeExerciseCount, inactive: row.inactiveExerciseCount }) }}
                <span v-if="row.childCount"> · {{ t('content.childSummary', { count: row.childCount }) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" :label="t('content.columns.status')" width="120" sortable="custom">
              <template #default="{ row }">
                <el-tag :type="row.status === 'active' ? 'success' : 'info'">
                  {{ t(`content.status.${row.status}`) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="updatedAt" :label="t('content.columns.updatedAt')" min-width="170" sortable="custom">
              <template #default="{ row }">
                {{ formatDate(row.updatedAt) }}
              </template>
            </el-table-column>
            <el-table-column :label="t('content.columns.actions')" fixed="right" width="220">
              <template #default="{ row }">
                <el-button link type="primary" @click="openSetDialog(row)">{{ t('content.actions.edit') }}</el-button>
                <el-button link type="primary" @click="filterExercisesBySet(row)">{{ t('content.actions.exercises') }}</el-button>
                <el-button link :type="row.status === 'active' ? 'warning' : 'success'" @click="toggleSetStatus(row)">
                  {{ row.status === 'active' ? t('content.actions.disable') : t('content.actions.enable') }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-row">
            <el-pagination
              v-model:current-page="setQuery.page"
              v-model:page-size="setQuery.pageSize"
              :total="setTotal"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleSetPageSizeChange"
              @current-change="loadExerciseSets"
            />
          </div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane :label="t('content.tabs.sentenceExercises')" name="exercises">
        <el-card shadow="never" class="filter-card">
          <el-form class="exercise-filter-form" :model="exerciseQuery" @submit.prevent>
            <el-form-item>
              <el-input
                v-model="exerciseQuery.keyword"
                clearable
                :prefix-icon="Search"
                :placeholder="t('content.exerciseKeyword')"
                @keyup.enter="searchExercises"
              />
            </el-form-item>
            <el-form-item>
              <el-tree-select
                v-model="exerciseQuery.exerciseSetId"
                class="full-input content-tree-select"
                clearable
                filterable
                check-strictly
                default-expand-all
                :data="exerciseSetTreeSelectOptions"
                :render-after-expand="false"
                popper-class="content-tree-select-popper"
                :placeholder="t('content.fields.exerciseSet')"
                @change="searchExercises"
              >
                <template #default="{ data }">
                  <span class="tree-select-node-label" :title="data.label">{{ data.nodeLabel }}</span>
                </template>
              </el-tree-select>
            </el-form-item>
            <el-form-item>
              <el-select v-model="exerciseQuery.status" clearable :placeholder="t('content.statusFilter')" @change="searchExercises">
                <el-option :label="t('content.status.all')" :value="''" />
                <el-option :label="t('content.status.active')" value="active" />
                <el-option :label="t('content.status.inactive')" value="inactive" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-select v-model="exerciseQuery.hasAudio" clearable :placeholder="t('content.audioFilter')" @change="searchExercises">
                <el-option :label="t('content.assetFilters.withAudio')" :value="true" />
                <el-option :label="t('content.assetFilters.missingAudio')" :value="false" />
              </el-select>
            </el-form-item>
            <el-form-item class="filter-actions">
              <el-button type="primary" :icon="Search" @click="searchExercises">{{ t('common.search') }}</el-button>
              <el-button @click="resetExerciseFilters">{{ t('content.reset') }}</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" class="table-card">
          <template #header>
            <div class="card-header">
              <div class="card-title-area">
                <div class="card-title-group">
                  <span>{{ t('content.exerciseTitle') }}</span>
                  <span>{{ t('content.total', { total: exerciseTotal }) }}</span>
                </div>
                <div class="card-actions">
                  <el-button type="primary" plain :icon="Plus" :disabled="sentenceExercisesReadonly" @click="openExerciseDialog()">
                    {{ t('content.actions.createExercise') }}
                  </el-button>
                  <el-button plain :icon="Upload" :disabled="sentenceExercisesReadonly" @click="openCsvImportDialog('sentence-exercises')">
                    {{ t('content.actions.importCsvWithTemplate') }}
                  </el-button>
                  <el-button plain :icon="Link" :disabled="sentenceExercisesReadonly" @click="openBulkBindDialog('exerciseAudio')">
                    {{ t('content.actions.bulkBindAudio') }}
                  </el-button>
                </div>
              </div>
              <div v-if="selectedExercises.length" class="batch-actions">
                <span>{{ t('content.batchStatus.selected', { count: selectedExercises.length }) }}</span>
                <el-button
                  size="small"
                  type="primary"
                  plain
                  :loading="batchAssignmentSubmitting"
                  :disabled="selectedExercisesReadonly"
                  @click="openBatchAssignmentDialog('exercises')"
                >
                  {{ t('content.actions.batchUpdateExerciseSets') }}
                </el-button>
                <el-button
                  size="small"
                  type="success"
                  plain
                  :loading="batchStatusSubmitting"
                  :disabled="selectedExercisesReadonly"
                  @click="submitBatchStatus('exercises', 'active')"
                >
                  {{ t('content.actions.batchEnable') }}
                </el-button>
                <el-button
                  size="small"
                  type="warning"
                  plain
                  :loading="batchStatusSubmitting"
                  :disabled="selectedExercisesReadonly"
                  @click="submitBatchStatus('exercises', 'inactive')"
                >
                  {{ t('content.actions.batchDisable') }}
                </el-button>
              </div>
            </div>
          </template>
          <el-alert
            v-if="sentenceExercisesReadonly"
            class="readonly-alert"
            type="warning"
            show-icon
            :closable="false"
            :title="t('content.parentReadonly.sentenceExercises')"
          />
          <el-table
            v-loading="exerciseLoading"
            :data="sentenceExercises"
            row-key="id"
            border
            :empty-text="t('content.emptyExercises')"
            @selection-change="setSelectedExercises"
            @sort-change="handleExerciseSortChange"
          >
            <el-table-column type="selection" width="48" />
            <el-table-column prop="id" label="ID" width="84" sortable="custom" />
            <el-table-column prop="hanziAnswer" :label="t('content.columns.answer')" min-width="220" sortable="custom">
              <template #default="{ row }">
                <div class="main-cell">
                  <strong>{{ row.hanziAnswer }}</strong>
                  <span>{{ row.pinyinPrompt || row.translationEn || row.translationRu || t('common.empty') }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column :label="t('content.columns.exerciseSet')" min-width="300">
              <template #default="{ row }">
                <div class="set-path-list">
                  <el-tag
                    v-for="assignment in sentenceExerciseSetTagItems(row)"
                    :key="`${row.id}-set-${assignment.id ?? assignment.label}`"
                    class="set-path-tag"
                    :closable="assignment.id !== null"
                    effect="plain"
                    size="small"
                    type="info"
                    @close="removeSentenceExerciseSetAssignment(row, assignment.id)"
                  >
                    {{ assignment.label }}
                  </el-tag>
                  <span v-if="!sentenceExerciseSetTagItems(row).length" class="muted-text">{{ t('common.empty') }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column :label="t('content.columns.audio')" min-width="160">
              <template #default="{ row }">
                <el-link v-if="row.audioUrl" :href="row.audioUrl" target="_blank" type="primary">#{{ row.audioZhAssetId }}</el-link>
                <span v-else>{{ t('common.empty') }}</span>
              </template>
            </el-table-column>
            <el-table-column :label="t('content.columns.options')" width="110">
              <template #default="{ row }">
                {{ row.wordOptions.length }}
              </template>
            </el-table-column>
            <el-table-column prop="sortOrder" :label="t('content.columns.sortOrder')" width="100" sortable="custom" />
            <el-table-column prop="status" :label="t('content.columns.status')" width="120" sortable="custom">
              <template #default="{ row }">
                <el-tag :type="row.status === 'active' ? 'success' : 'info'">
                  {{ t(`content.status.${row.status}`) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="updatedAt" :label="t('content.columns.updatedAt')" min-width="170" sortable="custom">
              <template #default="{ row }">
                {{ formatDate(row.updatedAt) }}
              </template>
            </el-table-column>
            <el-table-column :label="t('content.columns.actions')" fixed="right" width="160">
              <template #default="{ row }">
                <el-button link type="primary" :disabled="isSentenceExerciseReadonly(row)" @click="openExerciseDialog(row)">{{ t('content.actions.edit') }}</el-button>
                <el-button
                  link
                  :disabled="isSentenceExerciseReadonly(row)"
                  :type="row.status === 'active' ? 'warning' : 'success'"
                  @click="toggleExerciseStatus(row)"
                >
                  {{ row.status === 'active' ? t('content.actions.disable') : t('content.actions.enable') }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-row">
            <el-pagination
              v-model:current-page="exerciseQuery.page"
              v-model:page-size="exerciseQuery.pageSize"
              :total="exerciseTotal"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleExercisePageSizeChange"
              @current-change="loadSentenceExercises"
            />
          </div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane :label="t('content.tabs.videoMaterials')" name="materials">
        <el-card shadow="never" class="filter-card">
          <el-form class="material-filter-form" :model="materialQuery" @submit.prevent>
            <el-form-item>
              <el-input
                v-model="materialQuery.keyword"
                clearable
                :prefix-icon="Search"
                :placeholder="t('content.materialKeyword')"
                @keyup.enter="searchMaterials"
              />
            </el-form-item>
            <el-form-item>
              <el-select v-model="materialQuery.materialType" clearable :placeholder="t('content.materialTypeFilter')">
                <el-option v-for="type in materialTypes" :key="type" :label="t(`content.materialTypes.${type}`)" :value="type" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-select v-model="materialQuery.status" clearable :placeholder="t('content.statusFilter')">
                <el-option :label="t('content.status.all')" :value="''" />
                <el-option :label="t('content.status.active')" value="active" />
                <el-option :label="t('content.status.inactive')" value="inactive" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-select v-model="materialQuery.hasCover" clearable :placeholder="t('content.coverFilter')">
                <el-option :label="t('content.assetFilters.withCover')" :value="true" />
                <el-option :label="t('content.assetFilters.missingCover')" :value="false" />
              </el-select>
            </el-form-item>
            <el-form-item class="filter-actions">
              <el-button type="primary" :icon="Search" @click="searchMaterials">{{ t('common.search') }}</el-button>
              <el-button @click="resetMaterialFilters">{{ t('content.reset') }}</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" class="table-card">
          <template #header>
            <div class="card-header">
              <div class="card-title-area">
                <div class="card-title-group">
                  <span>{{ t('content.materialTitle') }}</span>
                  <span>{{ t('content.total', { total: materialTotal }) }}</span>
                </div>
                <div class="card-actions">
                  <el-button type="primary" plain :icon="Plus" @click="openMaterialDialog()">
                    {{ t('content.actions.createMaterial') }}
                  </el-button>
                  <el-button plain :icon="Upload" @click="openCsvImportDialog('video-materials')">
                    {{ t('content.actions.importCsvWithTemplate') }}
                  </el-button>
                  <el-button plain :icon="Link" @click="openBulkBindDialog('materialCover')">
                    {{ t('content.actions.bulkBindCover') }}
                  </el-button>
                </div>
              </div>
              <div v-if="selectedMaterials.length" class="batch-actions">
                <span>{{ t('content.batchStatus.selected', { count: selectedMaterials.length }) }}</span>
                <el-button size="small" type="success" plain :loading="batchStatusSubmitting" @click="submitBatchStatus('materials', 'active')">
                  {{ t('content.actions.batchEnable') }}
                </el-button>
                <el-button size="small" type="warning" plain :loading="batchStatusSubmitting" @click="submitBatchStatus('materials', 'inactive')">
                  {{ t('content.actions.batchDisable') }}
                </el-button>
              </div>
            </div>
          </template>
          <el-table
            class="hierarchy-table"
            v-loading="materialLoading"
            :data="videoMaterials"
            row-key="id"
            border
            :tree-props="treeTableProps"
            :empty-text="t('content.emptyMaterials')"
            @selection-change="setSelectedMaterials"
            @sort-change="handleMaterialSortChange"
          >
            <el-table-column type="selection" width="48" />
            <el-table-column prop="title" :label="t('content.columns.material')" min-width="260" sortable="custom">
              <template #default="{ row }">
                <div class="main-cell">
                  <strong>{{ row.title }}</strong>
                  <span>{{ row.description || t('common.empty') }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="id" label="ID" width="84" sortable="custom" />
            <el-table-column prop="materialType" :label="t('content.columns.type')" width="140" sortable="custom">
              <template #default="{ row }">
                {{ t(`content.materialTypes.${row.materialType}`) }}
              </template>
            </el-table-column>
            <el-table-column :label="t('content.columns.cover')" min-width="150">
              <template #default="{ row }">
                <el-link v-if="row.coverUrl" :href="row.coverUrl" target="_blank" type="primary">#{{ row.coverAssetId }}</el-link>
                <span v-else>{{ t('common.empty') }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="lineCount" :label="t('content.columns.lines')" width="160">
              <template #default="{ row }">
                {{ row.lineCount }}
                <span v-if="row.childCount"> · {{ t('content.childSummary', { count: row.childCount }) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" :label="t('content.columns.status')" width="120" sortable="custom">
              <template #default="{ row }">
                <el-tag :type="row.status === 'active' ? 'success' : 'info'">
                  {{ t(`content.status.${row.status}`) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="updatedAt" :label="t('content.columns.updatedAt')" min-width="170" sortable="custom">
              <template #default="{ row }">
                {{ formatDate(row.updatedAt) }}
              </template>
            </el-table-column>
            <el-table-column :label="t('content.columns.actions')" fixed="right" width="210">
              <template #default="{ row }">
                <el-button link type="primary" @click="openMaterialDialog(row)">{{ t('content.actions.edit') }}</el-button>
                <el-button link type="primary" @click="filterLinesByMaterial(row)">{{ t('content.actions.lines') }}</el-button>
                <el-button link :type="row.status === 'active' ? 'warning' : 'success'" @click="toggleMaterialStatus(row)">
                  {{ row.status === 'active' ? t('content.actions.disable') : t('content.actions.enable') }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-row">
            <el-pagination
              v-model:current-page="materialQuery.page"
              v-model:page-size="materialQuery.pageSize"
              :total="materialTotal"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleMaterialPageSizeChange"
              @current-change="loadVideoMaterials"
            />
          </div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane :label="t('content.tabs.dialogueLines')" name="lines">
        <el-card shadow="never" class="filter-card">
          <el-form class="line-filter-form" :model="lineQuery" @submit.prevent>
            <el-form-item>
              <el-input
                v-model="lineQuery.keyword"
                clearable
                :prefix-icon="Search"
                :placeholder="t('content.lineKeyword')"
                @keyup.enter="searchLines"
              />
            </el-form-item>
            <el-form-item>
              <el-tree-select
                v-model="lineQuery.materialId"
                class="full-input content-tree-select"
                clearable
                filterable
                check-strictly
                default-expand-all
                :data="videoMaterialTreeSelectOptions"
                :render-after-expand="false"
                popper-class="content-tree-select-popper"
                :placeholder="t('content.materialFilter')"
                @change="searchLines"
              >
                <template #default="{ data }">
                  <span class="tree-select-node-label" :title="data.label">{{ data.nodeLabel }}</span>
                </template>
              </el-tree-select>
            </el-form-item>
            <el-form-item>
              <el-select v-model="lineQuery.hasAudio" clearable :placeholder="t('content.audioFilter')" @change="searchLines">
                <el-option :label="t('content.assetFilters.withAudio')" :value="true" />
                <el-option :label="t('content.assetFilters.missingAudio')" :value="false" />
              </el-select>
            </el-form-item>
            <el-form-item class="filter-actions">
              <el-button type="primary" :icon="Search" @click="searchLines">{{ t('common.search') }}</el-button>
              <el-button @click="resetLineFilters">{{ t('content.reset') }}</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" class="table-card">
          <template #header>
            <div class="card-header">
              <div class="card-title-area">
                <div class="card-title-group">
                  <span>{{ t('content.lineTitle') }}</span>
                  <span>{{ t('content.total', { total: lineTotal }) }}</span>
                </div>
                <div class="card-actions">
                  <el-button type="primary" plain :icon="Plus" :disabled="dialogueLinesReadonly" @click="openLineDialog()">
                    {{ t('content.actions.createLine') }}
                  </el-button>
                  <el-button plain :icon="Upload" :disabled="dialogueLinesReadonly" @click="openCsvImportDialog('dialogue-lines')">
                    {{ t('content.actions.importCsvWithTemplate') }}
                  </el-button>
                  <el-button plain :icon="Link" :disabled="dialogueLinesReadonly" @click="openBulkBindDialog('lineAudio')">
                    {{ t('content.actions.bulkBindAudio') }}
                  </el-button>
                </div>
              </div>
            </div>
          </template>
          <el-alert
            v-if="dialogueLinesReadonly"
            class="readonly-alert"
            type="warning"
            show-icon
            :closable="false"
            :title="t('content.parentReadonly.dialogueLines')"
          />
          <el-table v-loading="lineLoading" :data="dialogueLines" row-key="id" border :empty-text="t('content.emptyLines')" @sort-change="handleLineSortChange">
            <el-table-column prop="id" label="ID" width="84" sortable="custom" />
            <el-table-column prop="lineNo" :label="t('content.columns.lineNo')" width="90" sortable="custom" />
            <el-table-column :label="t('content.columns.material')" min-width="180">
              <template #default="{ row }">
                {{ row.materialTitle || row.materialId }}
              </template>
            </el-table-column>
            <el-table-column prop="hanziText" :label="t('content.fields.hanziText')" min-width="240" sortable="custom">
              <template #default="{ row }">
                <div class="main-cell">
                  <strong>{{ row.hanziText }}</strong>
                  <span>{{ row.pinyinText || t('common.empty') }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="translationEn" :label="t('content.fields.translationEn')" min-width="180" show-overflow-tooltip />
            <el-table-column prop="translationRu" :label="t('content.fields.translationRu')" min-width="180" show-overflow-tooltip />
            <el-table-column :label="t('content.columns.audio')" min-width="150">
              <template #default="{ row }">
                <el-link v-if="row.audioUrl" :href="row.audioUrl" target="_blank" type="primary">#{{ row.audioAssetId }}</el-link>
                <span v-else>{{ t('common.empty') }}</span>
              </template>
            </el-table-column>
            <el-table-column :label="t('content.columns.timeRange')" width="150">
              <template #default="{ row }">
                {{ formatTimeRange(row.startMs, row.endMs) }}
              </template>
            </el-table-column>
            <el-table-column :label="t('content.columns.actions')" fixed="right" width="150">
              <template #default="{ row }">
                <el-button link type="primary" :disabled="isDialogueLineReadonly(row)" @click="openLineDialog(row)">{{ t('content.actions.edit') }}</el-button>
                <el-button link type="primary" @click="filterVocabByLine(row)">{{ t('content.tabs.lineVocab') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-row">
            <el-pagination
              v-model:current-page="lineQuery.page"
              v-model:page-size="lineQuery.pageSize"
              :total="lineTotal"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleLinePageSizeChange"
              @current-change="loadDialogueLines"
            />
          </div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane :label="t('content.tabs.lineVocab')" name="lineVocab">
        <el-card shadow="never" class="filter-card">
          <el-form class="line-vocab-filter-form" :model="lineVocabQuery" @submit.prevent>
            <el-form-item>
              <el-input
                v-model="lineVocabQuery.keyword"
                clearable
                :prefix-icon="Search"
                :placeholder="t('content.lineVocabKeyword')"
                @keyup.enter="searchLineVocab"
              />
            </el-form-item>
            <el-form-item>
              <el-tree-select
                v-model="lineVocabQuery.materialId"
                class="full-input content-tree-select"
                clearable
                filterable
                check-strictly
                default-expand-all
                :data="videoMaterialTreeSelectOptions"
                :render-after-expand="false"
                popper-class="content-tree-select-popper"
                :placeholder="t('content.materialFilter')"
                @change="handleLineVocabMaterialFilterChange"
              >
                <template #default="{ data }">
                  <span class="tree-select-node-label" :title="data.label">{{ data.nodeLabel }}</span>
                </template>
              </el-tree-select>
            </el-form-item>
            <el-form-item>
              <el-select v-model="lineVocabQuery.dialogueLineId" clearable filterable :placeholder="t('content.lineFilter')">
                <el-option v-for="line in lineOptions" :key="line.id" :label="lineOptionLabel(line)" :value="line.id" />
              </el-select>
            </el-form-item>
            <el-form-item class="filter-actions">
              <el-button type="primary" :icon="Search" @click="searchLineVocab">{{ t('common.search') }}</el-button>
              <el-button @click="resetLineVocabFilters">{{ t('content.reset') }}</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" class="table-card">
          <template #header>
            <div class="card-header">
              <div class="card-title-area">
                <div class="card-title-group">
                  <span>{{ t('content.lineVocabTitle') }}</span>
                  <span>{{ t('content.total', { total: lineVocabTotal }) }}</span>
                </div>
                <div class="card-actions">
                  <el-button type="primary" plain :icon="Plus" :disabled="lineVocabReadonly" @click="openLineVocabDialog()">
                    {{ t('content.actions.createLineVocab') }}
                  </el-button>
                  <el-button plain :icon="Upload" :disabled="lineVocabReadonly" @click="openCsvImportDialog('dialogue-line-vocab')">
                    {{ t('content.actions.importCsvWithTemplate') }}
                  </el-button>
                </div>
              </div>
            </div>
          </template>
          <el-alert
            v-if="lineVocabReadonly"
            class="readonly-alert"
            type="warning"
            show-icon
            :closable="false"
            :title="t('content.parentReadonly.lineVocab')"
          />
          <el-table v-loading="lineVocabLoading" :data="lineVocabRecords" row-key="id" border :empty-text="t('content.emptyLineVocab')" @sort-change="handleLineVocabSortChange">
            <el-table-column prop="id" label="ID" width="84" sortable="custom" />
            <el-table-column prop="wordText" :label="t('content.columns.word')" min-width="170" sortable="custom">
              <template #default="{ row }">
                <div class="main-cell">
                  <strong>{{ row.wordText }}</strong>
                  <span>{{ row.pinyin || t('common.empty') }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column :label="t('content.columns.line')" min-width="260">
              <template #default="{ row }">
                <div class="main-cell">
                  <strong>{{ row.materialTitle || row.materialId || t('common.empty') }} / #{{ row.lineNo || '-' }}</strong>
                  <span>{{ row.lineHanziText || t('common.empty') }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column :label="t('content.columns.linkedVocab')" min-width="150">
              <template #default="{ row }">
                {{ row.vocabItemHanzi || row.vocabItemId || t('common.empty') }}
              </template>
            </el-table-column>
            <el-table-column prop="meaningEn" :label="t('content.fields.meaningEn')" min-width="180" show-overflow-tooltip />
            <el-table-column prop="meaningRu" :label="t('content.fields.meaningRu')" min-width="180" show-overflow-tooltip />
            <el-table-column prop="explanation" :label="t('content.columns.explanation')" min-width="180" show-overflow-tooltip />
            <el-table-column :label="t('content.columns.actions')" fixed="right" width="140">
              <template #default="{ row }">
                <el-button link type="primary" :disabled="isLineVocabReadonly(row)" @click="openLineVocabDialog(row)">{{ t('content.actions.edit') }}</el-button>
                <el-button link type="danger" :disabled="isLineVocabReadonly(row)" @click="deleteLineVocab(row)">{{ t('content.actions.delete') }}</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-row">
            <el-pagination
              v-model:current-page="lineVocabQuery.page"
              v-model:page-size="lineVocabQuery.pageSize"
              :total="lineVocabTotal"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleLineVocabPageSizeChange"
              @current-change="loadLineVocab"
            />
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="listDialogVisible" :title="editingList ? t('content.editListTitle') : t('content.createListTitle')" width="620px">
      <el-form ref="listFormRef" :model="listForm" :rules="listRules" label-position="top">
        <el-form-item :label="t('content.fields.name')" prop="name">
          <el-input v-model="listForm.name" :placeholder="t('content.fields.listNamePlaceholder')" />
        </el-form-item>
        <div class="form-grid">
          <el-form-item :label="t('content.fields.listType')" prop="listType">
            <el-select v-model="listForm.listType" class="full-input">
              <el-option v-for="type in vocabTypes" :key="type" :label="t(`content.listTypes.${type}`)" :value="type" />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('content.fields.parentVocabList')">
            <el-tree-select
              v-model="listForm.parentId"
              class="full-input content-tree-select"
              clearable
              filterable
              check-strictly
              default-expand-all
              :data="parentVocabListTreeSelectOptions"
              :render-after-expand="false"
              popper-class="content-tree-select-popper"
            >
              <template #default="{ data }">
                <span class="tree-select-node-label" :title="data.label">{{ data.nodeLabel }}</span>
              </template>
            </el-tree-select>
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item :label="t('content.fields.level')">
            <el-input v-model="listForm.level" maxlength="20" />
          </el-form-item>
          <el-form-item :label="t('content.fields.sortOrder')" prop="sortOrder">
            <el-input-number v-model="listForm.sortOrder" class="full-input" :min="0" :max="999999" />
          </el-form-item>
          <el-form-item :label="t('content.fields.status')" prop="status">
            <el-radio-group v-model="listForm.status">
              <el-radio-button label="active">{{ t('content.status.active') }}</el-radio-button>
              <el-radio-button label="inactive">{{ t('content.status.inactive') }}</el-radio-button>
            </el-radio-group>
          </el-form-item>
        </div>
        <el-form-item :label="t('content.fields.description')">
          <el-input v-model="listForm.description" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="listDialogVisible = false">{{ t('content.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="submitList">{{ t('content.submit') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="itemDialogVisible" :title="editingItem ? t('content.editItemTitle') : t('content.createItemTitle')" width="720px">
      <el-form ref="itemFormRef" :model="itemForm" :rules="itemRules" label-position="top">
        <el-form-item :label="t('content.fields.vocabList')" prop="vocabListIds">
          <el-tree-select
            v-model="itemForm.vocabListIds"
            class="full-input content-tree-select"
            multiple
            collapse-tags
            collapse-tags-tooltip
            clearable
            filterable
            check-strictly
            default-expand-all
            :data="activeVocabListTreeSelectOptions"
            :render-after-expand="false"
            popper-class="content-tree-select-popper"
          >
            <template #default="{ data }">
              <span class="tree-select-node-label" :title="data.label">{{ data.nodeLabel }}</span>
            </template>
          </el-tree-select>
        </el-form-item>
        <div class="form-grid">
          <el-form-item :label="t('content.fields.hanzi')" prop="hanzi">
            <el-input v-model="itemForm.hanzi" />
          </el-form-item>
          <el-form-item :label="t('content.fields.pinyin')">
            <el-input v-model="itemForm.pinyin" />
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item :label="t('content.fields.sortOrder')" prop="sortOrder">
            <el-input-number v-model="itemForm.sortOrder" class="full-input" :min="0" :max="999999" />
          </el-form-item>
          <el-form-item :label="t('content.fields.audioAsset')">
            <div class="asset-picker">
              <el-select v-model="itemForm.audioAssetId" class="full-input" clearable filterable>
                <el-option
                  v-for="asset in audioOptions"
                  :key="asset.id"
                  :label="mediaOptionLabel(asset)"
                  :value="asset.id"
                />
              </el-select>
              <el-button :icon="Plus" @click="openUploadDialog('itemAudio', 'audio')">{{ t('content.actions.uploadAudio') }}</el-button>
            </div>
          </el-form-item>
        </div>
        <el-form-item :label="t('content.fields.meaningEn')">
          <el-input v-model="itemForm.meaningEn" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item :label="t('content.fields.meaningRu')">
          <el-input v-model="itemForm.meaningRu" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item :label="t('content.fields.exampleSentence')">
          <el-input v-model="itemForm.exampleSentence" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item :label="t('content.fields.status')" prop="status">
          <el-radio-group v-model="itemForm.status">
            <el-radio-button label="active">{{ t('content.status.active') }}</el-radio-button>
            <el-radio-button label="inactive">{{ t('content.status.inactive') }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemDialogVisible = false">{{ t('content.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" :disabled="itemFormReadonly" @click="submitItem">{{ t('content.submit') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="setDialogVisible" :title="editingSet ? t('content.editSetTitle') : t('content.createSetTitle')" width="620px">
      <el-form ref="setFormRef" :model="setForm" :rules="setRules" label-position="top">
        <el-form-item :label="t('content.fields.title')" prop="title">
          <el-input v-model="setForm.title" :placeholder="t('content.fields.setTitlePlaceholder')" />
        </el-form-item>
        <div class="form-grid">
          <el-form-item :label="t('content.fields.exerciseType')" prop="exerciseType">
            <el-select v-model="setForm.exerciseType" class="full-input">
              <el-option v-for="type in exerciseTypes" :key="type" :label="t(`content.exerciseTypes.${type}`)" :value="type" />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('content.fields.parentExerciseSet')">
            <el-tree-select
              v-model="setForm.parentId"
              class="full-input content-tree-select"
              clearable
              filterable
              check-strictly
              default-expand-all
              :data="parentExerciseSetTreeSelectOptions"
              :render-after-expand="false"
              popper-class="content-tree-select-popper"
            >
              <template #default="{ data }">
                <span class="tree-select-node-label" :title="data.label">{{ data.nodeLabel }}</span>
              </template>
            </el-tree-select>
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item :label="t('content.fields.level')">
            <el-input v-model="setForm.level" maxlength="20" />
          </el-form-item>
          <el-form-item :label="t('content.fields.status')" prop="status">
            <el-radio-group v-model="setForm.status">
              <el-radio-button label="active">{{ t('content.status.active') }}</el-radio-button>
              <el-radio-button label="inactive">{{ t('content.status.inactive') }}</el-radio-button>
            </el-radio-group>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="setDialogVisible = false">{{ t('content.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="submitSet">{{ t('content.submit') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="exerciseDialogVisible"
      :title="editingExercise ? t('content.editExerciseTitle') : t('content.createExerciseTitle')"
      width="760px"
    >
      <el-form ref="exerciseFormRef" :model="exerciseForm" :rules="exerciseRules" label-position="top">
        <el-form-item :label="t('content.fields.exerciseSet')" prop="exerciseSetIds">
          <el-tree-select
            v-model="exerciseForm.exerciseSetIds"
            class="full-input content-tree-select"
            multiple
            collapse-tags
            collapse-tags-tooltip
            clearable
            filterable
            check-strictly
            default-expand-all
            :data="exerciseFormSetTreeSelectOptions"
            :render-after-expand="false"
            popper-class="content-tree-select-popper"
            @change="handleExerciseSetChange"
          >
            <template #default="{ data }">
              <span class="tree-select-node-label" :title="data.label">{{ data.nodeLabel }}</span>
            </template>
          </el-tree-select>
        </el-form-item>
        <el-form-item :label="t('content.fields.sortOrder')" prop="sortOrder">
          <el-input-number v-model="exerciseForm.sortOrder" class="full-input" :min="0" :max="999999" />
        </el-form-item>
        <el-form-item :label="t('content.fields.hanziAnswer')" prop="hanziAnswer">
          <el-input v-model="exerciseForm.hanziAnswer" type="textarea" :rows="2" />
        </el-form-item>
        <div class="form-grid">
          <el-form-item :label="t('content.fields.pinyinPrompt')">
            <el-input v-model="exerciseForm.pinyinPrompt" />
          </el-form-item>
          <el-form-item :label="t('content.fields.audioZhAsset')">
            <div class="asset-picker">
              <el-select v-model="exerciseForm.audioZhAssetId" class="full-input" clearable filterable>
                <el-option
                  v-for="asset in audioOptions"
                  :key="asset.id"
                  :label="mediaOptionLabel(asset)"
                  :value="asset.id"
                />
              </el-select>
              <el-button :icon="Plus" @click="openUploadDialog('exerciseAudio', 'audio')">{{ t('content.actions.uploadAudio') }}</el-button>
            </div>
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item :label="t('content.fields.translationEn')">
            <el-input v-model="exerciseForm.translationEn" type="textarea" :rows="3" />
          </el-form-item>
          <el-form-item :label="t('content.fields.translationRu')">
            <el-input v-model="exerciseForm.translationRu" type="textarea" :rows="3" />
          </el-form-item>
        </div>
        <el-form-item :label="t('content.fields.wordOptions')">
          <el-input
            v-model="exerciseForm.wordOptionsText"
            type="textarea"
            :rows="5"
            :placeholder="t('content.fields.wordOptionsPlaceholder')"
          />
        </el-form-item>
        <el-form-item :label="t('content.fields.explanation')">
          <el-input v-model="exerciseForm.explanation" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item :label="t('content.fields.status')" prop="status">
          <el-radio-group v-model="exerciseForm.status">
            <el-radio-button label="active">{{ t('content.status.active') }}</el-radio-button>
            <el-radio-button label="inactive">{{ t('content.status.inactive') }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="exerciseDialogVisible = false">{{ t('content.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" :disabled="exerciseFormReadonly" @click="submitExercise">{{ t('content.submit') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="materialDialogVisible"
      :title="editingMaterial ? t('content.editMaterialTitle') : t('content.createMaterialTitle')"
      width="680px"
    >
      <el-form ref="materialFormRef" :model="materialForm" :rules="materialRules" label-position="top">
        <el-form-item :label="t('content.fields.materialTitle')" prop="title">
          <el-input v-model="materialForm.title" :placeholder="t('content.fields.materialTitlePlaceholder')" />
        </el-form-item>
        <div class="form-grid">
          <el-form-item :label="t('content.fields.materialType')" prop="materialType">
            <el-select v-model="materialForm.materialType" class="full-input" @change="handleMaterialTypeChange">
              <el-option v-for="type in materialTypes" :key="type" :label="t(`content.materialTypes.${type}`)" :value="type" />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('content.fields.parentMaterial')">
            <el-tree-select
              v-model="materialForm.parentId"
              class="full-input content-tree-select"
              clearable
              filterable
              check-strictly
              default-expand-all
              :data="parentVideoMaterialTreeSelectOptions"
              :render-after-expand="false"
              popper-class="content-tree-select-popper"
              @change="handleMaterialParentChange"
            >
              <template #default="{ data }">
                <span class="tree-select-node-label" :title="data.label">{{ data.nodeLabel }}</span>
              </template>
            </el-tree-select>
          </el-form-item>
        </div>
        <el-form-item :label="t('content.fields.coverAsset')">
          <div class="asset-picker">
            <el-select v-model="materialForm.coverAssetId" class="full-input" clearable filterable>
              <el-option
                v-for="asset in imageOptions"
                :key="asset.id"
                :label="mediaOptionLabel(asset)"
                :value="asset.id"
              />
            </el-select>
            <el-button :icon="Plus" @click="openUploadDialog('materialCover', 'image')">{{ t('content.actions.uploadImage') }}</el-button>
          </div>
        </el-form-item>
        <el-form-item :label="t('content.fields.description')">
          <el-input v-model="materialForm.description" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item :label="t('content.fields.status')" prop="status">
          <el-radio-group v-model="materialForm.status">
            <el-radio-button label="active">{{ t('content.status.active') }}</el-radio-button>
            <el-radio-button label="inactive">{{ t('content.status.inactive') }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="materialDialogVisible = false">{{ t('content.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" @click="submitMaterial">{{ t('content.submit') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="lineDialogVisible" :title="editingLine ? t('content.editLineTitle') : t('content.createLineTitle')" width="760px">
      <el-form ref="lineFormRef" :model="lineForm" :rules="lineRules" label-position="top">
        <el-form-item :label="t('content.fields.material')" prop="materialId">
          <el-tree-select
            v-model="lineForm.materialId"
            class="full-input content-tree-select"
            filterable
            check-strictly
            default-expand-all
            :data="activeVideoMaterialTreeSelectOptions"
            :render-after-expand="false"
            popper-class="content-tree-select-popper"
          >
            <template #default="{ data }">
              <span class="tree-select-node-label" :title="data.label">{{ data.nodeLabel }}</span>
            </template>
          </el-tree-select>
        </el-form-item>
        <div class="form-grid">
          <el-form-item :label="t('content.fields.lineNo')" prop="lineNo">
            <el-input-number v-model="lineForm.lineNo" class="full-input" :min="1" :max="999999" />
          </el-form-item>
          <el-form-item :label="t('content.fields.audioAsset')">
            <div class="asset-picker">
              <el-select v-model="lineForm.audioAssetId" class="full-input" clearable filterable>
                <el-option
                  v-for="asset in audioOptions"
                  :key="asset.id"
                  :label="mediaOptionLabel(asset)"
                  :value="asset.id"
                />
              </el-select>
              <el-button :icon="Plus" @click="openUploadDialog('lineAudio', 'audio')">{{ t('content.actions.uploadAudio') }}</el-button>
            </div>
          </el-form-item>
        </div>
        <el-form-item :label="t('content.fields.hanziText')" prop="hanziText">
          <el-input v-model="lineForm.hanziText" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item :label="t('content.fields.pinyinText')">
          <el-input v-model="lineForm.pinyinText" type="textarea" :rows="2" />
        </el-form-item>
        <div class="form-grid">
          <el-form-item :label="t('content.fields.translationEn')">
            <el-input v-model="lineForm.translationEn" type="textarea" :rows="3" />
          </el-form-item>
          <el-form-item :label="t('content.fields.translationRu')">
            <el-input v-model="lineForm.translationRu" type="textarea" :rows="3" />
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item :label="t('content.fields.startMs')">
            <el-input-number v-model="lineForm.startMs" class="full-input" :min="0" :controls="false" />
          </el-form-item>
          <el-form-item :label="t('content.fields.endMs')">
            <el-input-number v-model="lineForm.endMs" class="full-input" :min="0" :controls="false" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="lineDialogVisible = false">{{ t('content.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" :disabled="lineFormReadonly" @click="submitLine">{{ t('content.submit') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="lineVocabDialogVisible"
      :title="editingLineVocab ? t('content.editLineVocabTitle') : t('content.createLineVocabTitle')"
      width="720px"
    >
      <el-form ref="lineVocabFormRef" :model="lineVocabForm" :rules="lineVocabRules" label-position="top">
        <el-form-item :label="t('content.fields.dialogueLine')" prop="dialogueLineId">
          <el-select v-model="lineVocabForm.dialogueLineId" class="full-input" filterable>
            <el-option v-for="line in lineOptions" :key="line.id" :label="lineOptionLabel(line)" :value="line.id" />
          </el-select>
        </el-form-item>
        <div class="form-grid">
          <el-form-item :label="t('content.fields.vocabItem')">
            <el-select
              v-model="lineVocabForm.vocabItemId"
              class="full-input"
              clearable
              filterable
              @change="handleLineVocabItemChange"
            >
              <el-option
                v-for="item in vocabItemOptions"
                :key="item.id"
                :label="vocabItemOptionLabel(item)"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('content.fields.wordText')" prop="wordText">
            <el-input v-model="lineVocabForm.wordText" />
          </el-form-item>
        </div>
        <el-form-item :label="t('content.fields.pinyin')">
          <el-input v-model="lineVocabForm.pinyin" />
        </el-form-item>
        <div class="form-grid">
          <el-form-item :label="t('content.fields.meaningEn')">
            <el-input v-model="lineVocabForm.meaningEn" type="textarea" :rows="3" />
          </el-form-item>
          <el-form-item :label="t('content.fields.meaningRu')">
            <el-input v-model="lineVocabForm.meaningRu" type="textarea" :rows="3" />
          </el-form-item>
        </div>
        <el-form-item :label="t('content.fields.lineVocabExplanation')">
          <el-input v-model="lineVocabForm.explanation" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="lineVocabDialogVisible = false">{{ t('content.cancel') }}</el-button>
        <el-button type="primary" :loading="submitting" :disabled="lineVocabFormReadonly" @click="submitLineVocab">{{ t('content.submit') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="bulkBindDialogVisible" :title="bulkBindDialogTitle" width="640px">
      <div class="bulk-bind-content">
        <p class="bulk-bind-hint">{{ t('content.bulkBind.hint') }}</p>
        <el-input
          v-model="bulkBindForm.rowsText"
          type="textarea"
          :rows="10"
          :placeholder="t('content.bulkBind.placeholder')"
        />
      </div>
      <template #footer>
        <el-button @click="bulkBindDialogVisible = false">{{ t('content.cancel') }}</el-button>
        <el-button type="primary" :loading="bulkBindSubmitting" @click="submitBulkBind">
          {{ t('content.submit') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchAssignmentDialogVisible" :title="batchAssignmentDialogTitle" width="620px">
      <el-alert class="batch-assignment-alert" type="info" show-icon :closable="false" :title="t('content.batchAssignments.replaceHint')" />
      <el-form label-position="top">
        <el-form-item :label="batchAssignmentFieldLabel" required>
          <el-tree-select
            v-model="batchAssignmentForm.targetIds"
            class="full-input content-tree-select"
            filterable
            multiple
            collapse-tags
            collapse-tags-tooltip
            check-strictly
            default-expand-all
            :data="batchAssignmentTreeOptions"
            :render-after-expand="false"
            popper-class="content-tree-select-popper"
            :placeholder="batchAssignmentPlaceholder"
          >
            <template #default="{ data }">
              <span class="tree-select-node-label" :title="data.label">{{ data.nodeLabel }}</span>
            </template>
          </el-tree-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchAssignmentDialogVisible = false">{{ t('content.cancel') }}</el-button>
        <el-button type="primary" :loading="batchAssignmentSubmitting" @click="submitBatchAssignment">
          {{ t('content.submit') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="csvImportDialogVisible" :title="t('content.importCsvTitle')" width="620px">
      <el-form label-position="top">
        <el-form-item :label="t('content.fields.importType')">
          <el-input class="full-input" :model-value="t(`content.importTypes.${csvImportForm.importType}`)" disabled />
        </el-form-item>
        <el-form-item v-if="csvImportForm.importType === 'vocab-items'" :label="t('content.fields.vocabList')">
          <el-tree-select
            v-model="csvImportForm.contextIds"
            class="full-input content-tree-select"
            filterable
            multiple
            collapse-tags
            collapse-tags-tooltip
            check-strictly
            default-expand-all
            :data="activeVocabListTreeSelectOptions"
            :render-after-expand="false"
            popper-class="content-tree-select-popper"
            :placeholder="t('content.importContext.placeholders.vocabList')"
            @change="clearCsvImportError"
          >
            <template #default="{ data }">
              <span class="tree-select-node-label" :title="data.label">{{ data.nodeLabel }}</span>
            </template>
          </el-tree-select>
        </el-form-item>
        <el-form-item v-if="csvImportForm.importType === 'sentence-exercises'" :label="t('content.fields.exerciseSet')">
          <el-tree-select
            v-model="csvImportForm.contextIds"
            class="full-input content-tree-select"
            filterable
            multiple
            collapse-tags
            collapse-tags-tooltip
            check-strictly
            default-expand-all
            :data="activeExerciseSetTreeSelectOptions"
            :render-after-expand="false"
            popper-class="content-tree-select-popper"
            :placeholder="t('content.importContext.placeholders.exerciseSet')"
            @change="clearCsvImportError"
          >
            <template #default="{ data }">
              <span class="tree-select-node-label" :title="data.label">{{ data.nodeLabel }}</span>
            </template>
          </el-tree-select>
        </el-form-item>
        <el-form-item v-if="csvImportForm.importType === 'dialogue-lines'" :label="t('content.fields.material')" required>
          <el-tree-select
            v-model="csvImportForm.contextId"
            class="full-input content-tree-select"
            filterable
            check-strictly
            default-expand-all
            :data="activeVideoMaterialTreeSelectOptions"
            :render-after-expand="false"
            popper-class="content-tree-select-popper"
            :placeholder="t('content.importContext.placeholders.material')"
            @change="clearCsvImportError"
          >
            <template #default="{ data }">
              <span class="tree-select-node-label" :title="data.label">{{ data.nodeLabel }}</span>
            </template>
          </el-tree-select>
        </el-form-item>
        <el-form-item v-if="csvImportForm.importType === 'dialogue-line-vocab'" :label="t('content.fields.dialogueLine')" required>
          <el-select v-model="csvImportForm.contextId" class="full-input" filterable :placeholder="t('content.importContext.placeholders.dialogueLine')" @change="clearCsvImportError">
            <el-option v-for="line in lineOptions" :key="line.id" :label="lineOptionLabel(line)" :value="line.id" />
          </el-select>
        </el-form-item>
        <el-alert
          v-if="csvImportMissingContext"
          class="readonly-alert"
          type="warning"
          show-icon
          :closable="false"
          :title="t('content.importContext.required', { target: importContextTarget(csvImportForm.importType) })"
        />
        <el-alert
          v-if="csvImportContextReadonly"
          class="readonly-alert"
          type="warning"
          show-icon
          :closable="false"
          :title="t('content.parentReadonly.importDisabled')"
        />
        <el-alert
          v-if="csvImportLastError"
          class="import-error-alert"
          type="error"
          show-icon
          :closable="true"
          :title="csvImportLastError"
          :description="csvImportLastErrorHint"
          @close="clearCsvImportError"
        />
        <el-form-item :label="t('content.fields.file')">
          <el-upload
            v-model:file-list="csvImportForm.fileList"
            drag
            accept=".csv,text/csv"
            :auto-upload="false"
            :limit="1"
            :disabled="csvImportBlocked"
            :on-exceed="handleCsvImportExceed"
          >
            <el-icon class="upload-icon"><UploadFilled /></el-icon>
            <div class="upload-text">{{ t('content.importDropText') }}</div>
          </el-upload>
        </el-form-item>
        <el-form-item>
          <el-button
            plain
            :icon="Download"
            :loading="templateDownloading"
            :disabled="csvImportBlocked"
            @click="downloadTemplate(csvImportForm.importType)"
          >
            {{ t('content.actions.downloadTemplate') }}
          </el-button>
        </el-form-item>
        <p class="bulk-bind-hint">{{ t('content.importCsvHint') }}</p>
      </el-form>
      <template #footer>
        <el-button @click="csvImportDialogVisible = false">{{ t('content.cancel') }}</el-button>
        <el-button type="primary" :loading="csvImportSubmitting" :disabled="csvImportBlocked" @click="submitCsvImport">
          {{ t('content.submit') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="uploadDialogVisible" :title="t('content.uploadMediaTitle')" width="560px">
      <el-form ref="uploadFormRef" :model="uploadForm" :rules="uploadRules" label-position="top">
        <div class="form-grid">
          <el-form-item :label="t('content.fields.mediaType')" prop="mediaType">
            <el-select v-model="uploadForm.mediaType" class="full-input" :disabled="uploadTarget !== 'general'">
              <el-option :label="t('content.mediaTypes.audio')" value="audio" />
              <el-option :label="t('content.mediaTypes.image')" value="image" />
              <el-option :label="t('content.mediaTypes.video')" value="video" />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('content.fields.language')">
            <el-select v-model="uploadForm.language" class="full-input" clearable>
              <el-option :label="t('content.languages.zh')" value="zh" />
              <el-option :label="t('content.languages.ru')" value="ru" />
              <el-option :label="t('content.languages.en')" value="en" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item :label="t('content.fields.durationMs')">
          <el-input-number v-model="uploadForm.durationMs" class="full-input" :min="0" :controls="false" />
        </el-form-item>
        <el-form-item :label="t('content.fields.file')" prop="fileList">
          <el-upload
            v-model:file-list="uploadForm.fileList"
            drag
            :accept="uploadAccept"
            :auto-upload="false"
            :limit="1"
            :on-exceed="handleUploadExceed"
          >
            <el-icon class="upload-icon"><UploadFilled /></el-icon>
            <div class="upload-text">{{ t('content.uploadDropText') }}</div>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadDialogVisible = false">{{ t('content.cancel') }}</el-button>
        <el-button type="primary" :loading="uploading" @click="submitUpload">{{ t('content.actions.uploadMedia') }}</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type UploadUserFile } from 'element-plus'
import { Download, Link, Plus, Refresh, Search, Upload, UploadFilled } from '@element-plus/icons-vue'
import {
  bindAdminDialogueLineAudio,
  bindAdminSentenceExerciseAudio,
  bindAdminVideoMaterialCover,
  bindAdminVocabItemAudio,
  createAdminDialogueLine,
  createAdminDialogueLineVocab,
  createAdminExerciseSet,
  createAdminSentenceExercise,
  createAdminVideoMaterial,
  createAdminVocabItem,
  createAdminVocabList,
  deleteAdminMediaAsset,
  fetchAdminDialogueLines,
  fetchAdminDialogueLineVocab,
  fetchAdminExerciseSets,
  fetchAdminMediaAssets,
  fetchAdminSentenceExercises,
  fetchAdminVideoMaterials,
  fetchAdminVocabItems,
  fetchAdminVocabLists,
  updateAdminDialogueLine,
  updateAdminDialogueLineVocab,
  deleteAdminDialogueLineVocab,
  downloadAdminContentImportTemplate,
  importAdminContentCsv,
  updateAdminExerciseSet,
  updateAdminExerciseSetStatus,
  updateAdminExerciseSetStatuses,
  updateAdminMediaAssetStatus,
  updateAdminMediaAssetStatuses,
  updateAdminSentenceExercise,
  updateAdminSentenceExerciseSetAssignments,
  updateAdminSentenceExerciseStatus,
  updateAdminSentenceExerciseStatuses,
  updateAdminVideoMaterial,
  updateAdminVideoMaterialStatus,
  updateAdminVideoMaterialStatuses,
  updateAdminVocabItem,
  updateAdminVocabItemListAssignments,
  updateAdminVocabItemStatus,
  updateAdminVocabItemStatuses,
  updateAdminVocabList,
  updateAdminVocabListStatus,
  updateAdminVocabListStatuses,
  uploadAdminMediaAsset
} from '@/api/content'
import { ApiError } from '@/api/http'
import type {
  AdminBatchBindMediaAssetPayload,
  AdminBatchContentStatusResult,
  AdminContentImportType,
  AdminDialogueLine,
  AdminDialogueLineQuery,
  AdminDialogueLineVocab,
  AdminDialogueLineVocabQuery,
  AdminExerciseSet,
  AdminExerciseSetQuery,
  AdminMediaAsset,
  AdminMediaAssetQuery,
  AdminSentenceExercise,
  AdminSentenceExerciseQuery,
  AdminVocabItem,
  AdminVocabItemQuery,
  AdminVocabList,
  AdminVocabListQuery,
  AdminVideoMaterial,
  AdminVideoMaterialQuery,
  ContentStatus,
  ExerciseType,
  MediaLanguage,
  MediaType,
  VideoMaterialType,
  VocabListType
} from '@/types/api'
import { applyTableSort, type TableSortChange } from '@/utils/tableSort'

const { t, locale } = useI18n()
const route = useRoute()
const vocabTypes: VocabListType[] = ['HSK', 'YCT', 'category', 'professional', 'custom']
const exerciseTypes: ExerciseType[] = ['audio_order', 'audio_dictation', 'pinyin_dictation', 'translation_order']
const materialTypes: VideoMaterialType[] = ['drama', 'short_video', 'cartoon']
type UploadTarget = 'general' | 'itemAudio' | 'exerciseAudio' | 'lineAudio' | 'materialCover'
type BulkBindTarget = 'itemAudio' | 'exerciseAudio' | 'lineAudio' | 'materialCover'
type ContentTab = 'lists' | 'items' | 'media' | 'sets' | 'exercises' | 'materials' | 'lines' | 'lineVocab'
type BatchStatusTarget = 'lists' | 'items' | 'media' | 'sets' | 'exercises' | 'materials'
type BatchAssignmentTarget = 'items' | 'exercises'
type AdminVocabListTreeNode = AdminVocabList & { children?: AdminVocabListTreeNode[] }
type AdminExerciseSetTreeNode = AdminExerciseSet & { children?: AdminExerciseSetTreeNode[] }
type AdminVideoMaterialTreeNode = AdminVideoMaterial & { children?: AdminVideoMaterialTreeNode[] }
interface AssignmentTagItem {
  id: number | null
  label: string
}
interface ContentTreeSelectOption {
  value: number
  label: string
  nodeLabel: string
  children?: ContentTreeSelectOption[]
}

const contextImportTypes = new Set<AdminContentImportType>([
  'dialogue-lines',
  'dialogue-line-vocab'
])
const treeTableProps = { children: 'children' }
const treeChildPageSize = 100
const defaultContentStatus: ContentStatus = 'active'

const activeTab = ref<ContentTab>('lists')
const listLoading = ref(false)
const itemLoading = ref(false)
const mediaLoading = ref(false)
const setLoading = ref(false)
const exerciseLoading = ref(false)
const materialLoading = ref(false)
const lineLoading = ref(false)
const lineVocabLoading = ref(false)
const activeTabLoading = computed(() => {
  if (activeTab.value === 'lists') {
    return listLoading.value
  }
  if (activeTab.value === 'items') {
    return itemLoading.value
  }
  if (activeTab.value === 'media') {
    return mediaLoading.value
  }
  if (activeTab.value === 'sets') {
    return setLoading.value
  }
  if (activeTab.value === 'exercises') {
    return exerciseLoading.value
  }
  if (activeTab.value === 'materials') {
    return materialLoading.value
  }
  if (activeTab.value === 'lines') {
    return lineLoading.value
  }
  return lineVocabLoading.value
})
const submitting = ref(false)
const uploading = ref(false)
const bulkBindSubmitting = ref(false)
const csvImportSubmitting = ref(false)
const templateDownloading = ref(false)
const batchStatusSubmitting = ref(false)
const batchAssignmentSubmitting = ref(false)
const listDialogVisible = ref(false)
const itemDialogVisible = ref(false)
const uploadDialogVisible = ref(false)
const uploadTarget = ref<UploadTarget>('general')
const bulkBindDialogVisible = ref(false)
const batchAssignmentDialogVisible = ref(false)
const batchAssignmentTarget = ref<BatchAssignmentTarget>('items')
const csvImportDialogVisible = ref(false)
const setDialogVisible = ref(false)
const exerciseDialogVisible = ref(false)
const materialDialogVisible = ref(false)
const lineDialogVisible = ref(false)
const lineVocabDialogVisible = ref(false)
const vocabLists = ref<AdminVocabListTreeNode[]>([])
const listOptions = ref<AdminVocabList[]>([])
const vocabItems = ref<AdminVocabItem[]>([])
const vocabItemOptions = ref<AdminVocabItem[]>([])
const mediaAssets = ref<AdminMediaAsset[]>([])
const audioOptions = ref<AdminMediaAsset[]>([])
const imageOptions = ref<AdminMediaAsset[]>([])
const exerciseSets = ref<AdminExerciseSetTreeNode[]>([])
const setOptions = ref<AdminExerciseSet[]>([])
const sentenceExercises = ref<AdminSentenceExercise[]>([])
const videoMaterials = ref<AdminVideoMaterialTreeNode[]>([])
const materialOptions = ref<AdminVideoMaterial[]>([])
const dialogueLines = ref<AdminDialogueLine[]>([])
const lineOptions = ref<AdminDialogueLine[]>([])
const lineVocabRecords = ref<AdminDialogueLineVocab[]>([])
const selectedLists = ref<AdminVocabList[]>([])
const selectedItems = ref<AdminVocabItem[]>([])
const selectedMediaAssets = ref<AdminMediaAsset[]>([])
const selectedSets = ref<AdminExerciseSet[]>([])
const selectedExercises = ref<AdminSentenceExercise[]>([])
const selectedMaterials = ref<AdminVideoMaterial[]>([])
const listTotal = ref(0)
const itemTotal = ref(0)
const mediaTotal = ref(0)
const setTotal = ref(0)
const exerciseTotal = ref(0)
const materialTotal = ref(0)
const lineTotal = ref(0)
const lineVocabTotal = ref(0)
const editingList = ref<AdminVocabList | null>(null)
const editingItem = ref<AdminVocabItem | null>(null)
const editingSet = ref<AdminExerciseSet | null>(null)
const editingExercise = ref<AdminSentenceExercise | null>(null)
const editingMaterial = ref<AdminVideoMaterial | null>(null)
const editingLine = ref<AdminDialogueLine | null>(null)
const editingLineVocab = ref<AdminDialogueLineVocab | null>(null)
const listFormRef = ref<FormInstance>()
const itemFormRef = ref<FormInstance>()
const uploadFormRef = ref<FormInstance>()
const setFormRef = ref<FormInstance>()
const exerciseFormRef = ref<FormInstance>()
const materialFormRef = ref<FormInstance>()
const lineFormRef = ref<FormInstance>()
const lineVocabFormRef = ref<FormInstance>()

const uploadAccept = computed(() => {
  if (uploadForm.mediaType === 'audio') {
    return 'audio/*,.mp3,.wav,.m4a,.ogg,.aac,.webm'
  }
  if (uploadForm.mediaType === 'image') {
    return 'image/*,.jpg,.jpeg,.png,.webp,.gif'
  }
  return 'video/*,.mp4,.webm,.mov'
})

const listQuery = reactive<AdminVocabListQuery>({
  page: 1,
  pageSize: 20,
  keyword: '',
  listType: '',
  level: '',
  status: defaultContentStatus,
  sortBy: '',
  sortDirection: ''
})

const itemQuery = reactive<AdminVocabItemQuery>({
  page: 1,
  pageSize: 20,
  vocabListId: null,
  keyword: '',
  status: defaultContentStatus,
  hasAudio: null,
  sortBy: '',
  sortDirection: ''
})

const mediaQuery = reactive<AdminMediaAssetQuery>({
  page: 1,
  pageSize: 20,
  keyword: '',
  mediaType: 'audio',
  language: '',
  status: defaultContentStatus,
  sortBy: '',
  sortDirection: ''
})

const setQuery = reactive<AdminExerciseSetQuery>({
  page: 1,
  pageSize: 20,
  keyword: '',
  exerciseType: '',
  level: '',
  status: defaultContentStatus,
  sortBy: '',
  sortDirection: ''
})

const exerciseQuery = reactive<AdminSentenceExerciseQuery>({
  page: 1,
  pageSize: 20,
  exerciseSetId: null,
  keyword: '',
  exerciseType: '',
  status: defaultContentStatus,
  hasAudio: null,
  sortBy: '',
  sortDirection: ''
})

const materialQuery = reactive<AdminVideoMaterialQuery>({
  page: 1,
  pageSize: 20,
  keyword: '',
  materialType: '',
  status: defaultContentStatus,
  hasCover: null,
  sortBy: '',
  sortDirection: ''
})

const lineQuery = reactive<AdminDialogueLineQuery>({
  page: 1,
  pageSize: 20,
  materialId: null,
  keyword: '',
  hasAudio: null,
  sortBy: '',
  sortDirection: ''
})

const lineVocabQuery = reactive<AdminDialogueLineVocabQuery>({
  page: 1,
  pageSize: 20,
  dialogueLineId: null,
  materialId: null,
  keyword: '',
  sortBy: '',
  sortDirection: ''
})

const listForm = reactive<{
  name: string
  parentId: number | null
  listType: VocabListType
  level: string
  description: string
  sortOrder: number
  status: ContentStatus
}>({
  name: '',
  parentId: null,
  listType: 'HSK',
  level: '',
  description: '',
  sortOrder: 0,
  status: 'active'
})

const itemForm = reactive<{
  vocabListId: number | null
  vocabListIds: number[]
  hanzi: string
  pinyin: string
  meaningEn: string
  meaningRu: string
  exampleSentence: string
  audioAssetId: number | null
  sortOrder: number
  status: ContentStatus
}>({
  vocabListId: null,
  vocabListIds: [],
  hanzi: '',
  pinyin: '',
  meaningEn: '',
  meaningRu: '',
  exampleSentence: '',
  audioAssetId: null,
  sortOrder: 0,
  status: 'active'
})

const uploadForm = reactive<{
  mediaType: MediaType
  language: MediaLanguage | ''
  durationMs: number | null
  fileList: UploadUserFile[]
}>({
  mediaType: 'audio',
  language: 'zh',
  durationMs: null,
  fileList: []
})

const bulkBindForm = reactive<{
  target: BulkBindTarget
  rowsText: string
}>({
  target: 'itemAudio',
  rowsText: ''
})

const bulkBindDialogTitle = computed(() => t(`content.bulkBind.targets.${bulkBindForm.target}`))

const csvImportForm = reactive<{
  importType: AdminContentImportType
  contextId: number | null
  contextIds: number[]
  fileList: UploadUserFile[]
}>({
  importType: 'vocab-lists',
  contextId: null,
  contextIds: [],
  fileList: []
})
const csvImportLastError = ref('')
const csvImportLastErrorHint = ref('')

const setForm = reactive<{
  title: string
  parentId: number | null
  exerciseType: ExerciseType
  level: string
  status: ContentStatus
}>({
  title: '',
  parentId: null,
  exerciseType: 'audio_order',
  level: '',
  status: 'active'
})

const exerciseForm = reactive<{
  exerciseSetId: number | null
  exerciseSetIds: number[]
  exerciseType: ExerciseType
  hanziAnswer: string
  pinyinPrompt: string
  translationEn: string
  translationRu: string
  audioZhAssetId: number | null
  explanation: string
  sortOrder: number
  status: ContentStatus
  wordOptionsText: string
}>({
  exerciseSetId: null,
  exerciseSetIds: [],
  exerciseType: 'audio_order',
  hanziAnswer: '',
  pinyinPrompt: '',
  translationEn: '',
  translationRu: '',
  audioZhAssetId: null,
  explanation: '',
  sortOrder: 0,
  status: 'active',
  wordOptionsText: ''
})

const materialForm = reactive<{
  title: string
  parentId: number | null
  materialType: VideoMaterialType
  description: string
  coverAssetId: number | null
  status: ContentStatus
}>({
  title: '',
  parentId: null,
  materialType: 'drama',
  description: '',
  coverAssetId: null,
  status: 'active'
})

const lineForm = reactive<{
  materialId: number | null
  lineNo: number
  hanziText: string
  pinyinText: string
  translationEn: string
  translationRu: string
  audioAssetId: number | null
  startMs: number | null
  endMs: number | null
}>({
  materialId: null,
  lineNo: 1,
  hanziText: '',
  pinyinText: '',
  translationEn: '',
  translationRu: '',
  audioAssetId: null,
  startMs: null,
  endMs: null
})

const lineVocabForm = reactive<{
  dialogueLineId: number | null
  vocabItemId: number | null
  wordText: string
  pinyin: string
  meaningEn: string
  meaningRu: string
  explanation: string
}>({
  dialogueLineId: null,
  vocabItemId: null,
  wordText: '',
  pinyin: '',
  meaningEn: '',
  meaningRu: '',
  explanation: ''
})

const batchAssignmentForm = reactive<{
  targetIds: number[]
}>({
  targetIds: []
})

const appliedItemVocabListId = ref<number | null>(null)
const appliedExerciseSetId = ref<number | null>(null)
const appliedLineMaterialId = ref<number | null>(null)
const appliedLineVocabMaterialId = ref<number | null>(null)
const appliedLineVocabDialogueLineId = ref<number | null>(null)

const currentVocabList = computed(() => findVocabList(appliedItemVocabListId.value))
const currentExerciseSet = computed(() => findExerciseSet(appliedExerciseSetId.value))
const currentLineMaterial = computed(() => findVideoMaterial(appliedLineMaterialId.value))
const currentLineVocabMaterial = computed(() => {
  const line = findDialogueLine(appliedLineVocabDialogueLineId.value)
  return findVideoMaterial(line?.materialId || appliedLineVocabMaterialId.value)
})
const vocabListTreeSelectOptions = computed(() => buildVocabListTreeSelectOptions(listOptions.value))
const exerciseSetTreeSelectOptions = computed(() => buildExerciseSetTreeSelectOptions(setOptions.value))
const videoMaterialTreeSelectOptions = computed(() => buildVideoMaterialTreeSelectOptions(materialOptions.value))
const vocabListRecordById = computed(() => new Map(listOptions.value.map(record => [record.id, record])))
const exerciseSetRecordById = computed(() => new Map(setOptions.value.map(record => [record.id, record])))
const activeVocabListTreeSelectOptions = computed(() =>
  vocabListTreeSelectOptions.value
)
const activeExerciseSetTreeSelectOptions = computed(() =>
  exerciseSetTreeSelectOptions.value
)
const batchAssignmentTreeOptions = computed(() =>
  batchAssignmentTarget.value === 'items' ? activeVocabListTreeSelectOptions.value : activeExerciseSetTreeSelectOptions.value
)
const batchAssignmentDialogTitle = computed(() =>
  t(batchAssignmentTarget.value === 'items' ? 'content.batchAssignments.titleItems' : 'content.batchAssignments.titleExercises')
)
const batchAssignmentFieldLabel = computed(() =>
  t(batchAssignmentTarget.value === 'items' ? 'content.batchAssignments.fieldVocabLists' : 'content.batchAssignments.fieldExerciseSets')
)
const batchAssignmentPlaceholder = computed(() =>
  t(batchAssignmentTarget.value === 'items' ? 'content.batchAssignments.placeholders.vocabLists' : 'content.batchAssignments.placeholders.exerciseSets')
)
const exerciseFormSetTreeSelectOptions = computed(() =>
  exerciseSetTreeSelectOptions.value
)
const activeVideoMaterialTreeSelectOptions = computed(() =>
  buildVideoMaterialTreeSelectOptions(materialOptions.value.filter(material => !isVideoMaterialHierarchyInactive(material.id)))
)
const parentVocabListTreeSelectOptions = computed(() =>
  buildVocabListTreeSelectOptions(
    listOptions.value.filter(list => list.listType === listForm.listType && !isVocabListHierarchyInactive(list.id) && !isVocabListInSubtree(list.id, editingList.value?.id))
  )
)
const parentExerciseSetTreeSelectOptions = computed(() =>
  buildExerciseSetTreeSelectOptions(
    setOptions.value.filter(set => set.exerciseType === setForm.exerciseType && !isExerciseSetHierarchyInactive(set.id) && !isExerciseSetInSubtree(set.id, editingSet.value?.id))
  )
)
const parentVideoMaterialTreeSelectOptions = computed(() =>
  buildVideoMaterialTreeSelectOptions(
    materialOptions.value.filter(material => material.materialType === materialForm.materialType && !isVideoMaterialHierarchyInactive(material.id) && !isVideoMaterialInSubtree(material.id, editingMaterial.value?.id))
  )
)
const vocabItemsReadonly = computed(() => false)
const sentenceExercisesReadonly = computed(() => false)
const dialogueLinesReadonly = computed(() => isVideoMaterialHierarchyInactive(currentLineMaterial.value?.id))
const lineVocabReadonly = computed(() => isVideoMaterialHierarchyInactive(currentLineVocabMaterial.value?.id))
const selectedItemsReadonly = computed(() => false)
const selectedExercisesReadonly = computed(() => false)
const itemFormReadonly = computed(() => false)
const exerciseFormReadonly = computed(() => false)
const lineFormReadonly = computed(() => isVideoMaterialHierarchyInactive(lineForm.materialId))
const lineVocabFormReadonly = computed(() => {
  const line = findDialogueLine(lineVocabForm.dialogueLineId)
  return isVideoMaterialHierarchyInactive(line?.materialId)
})
const csvImportRequiresContext = computed(() => requiresImportContext(csvImportForm.importType))
const csvImportSelectedContextIds = computed(() => selectedImportContextIds(csvImportForm.importType))
const csvImportMissingContext = computed(() => csvImportRequiresContext.value && csvImportSelectedContextIds.value.length === 0)
const csvImportContextReadonly = computed(() => csvImportSelectedContextIds.value.some(id => isImportContextInactive(csvImportForm.importType, id)))
const csvImportBlocked = computed(() => csvImportMissingContext.value || csvImportContextReadonly.value)

const listRules = computed<FormRules>(() => ({
  name: [
    { required: true, message: t('content.validation.nameRequired'), trigger: 'blur' },
    { max: 100, message: t('content.validation.nameTooLong'), trigger: 'blur' }
  ],
  listType: [{ required: true, message: t('content.validation.typeRequired'), trigger: 'change' }],
  sortOrder: [{ required: true, message: t('content.validation.sortRequired'), trigger: 'change' }]
}))

const itemRules = computed<FormRules>(() => ({
  hanzi: [
    { required: true, message: t('content.validation.hanziRequired'), trigger: 'blur' },
    { max: 100, message: t('content.validation.hanziTooLong'), trigger: 'blur' }
  ],
  sortOrder: [{ required: true, message: t('content.validation.sortRequired'), trigger: 'change' }]
}))

const uploadRules = computed<FormRules>(() => ({
  mediaType: [{ required: true, message: t('content.validation.mediaTypeRequired'), trigger: 'change' }],
  fileList: [{ required: true, message: t('content.validation.fileRequired'), trigger: 'change' }]
}))

const setRules = computed<FormRules>(() => ({
  title: [
    { required: true, message: t('content.validation.titleRequired'), trigger: 'blur' },
    { max: 100, message: t('content.validation.nameTooLong'), trigger: 'blur' }
  ],
  exerciseType: [{ required: true, message: t('content.validation.typeRequired'), trigger: 'change' }]
}))

const exerciseRules = computed<FormRules>(() => ({
  hanziAnswer: [{ required: true, message: t('content.validation.answerRequired'), trigger: 'blur' }],
  sortOrder: [{ required: true, message: t('content.validation.sortRequired'), trigger: 'change' }]
}))

const materialRules = computed<FormRules>(() => ({
  title: [
    { required: true, message: t('content.validation.titleRequired'), trigger: 'blur' },
    { max: 200, message: t('content.validation.nameTooLong'), trigger: 'blur' }
  ],
  materialType: [{ required: true, message: t('content.validation.typeRequired'), trigger: 'change' }]
}))

const lineRules = computed<FormRules>(() => ({
  materialId: [{ required: true, message: t('content.validation.materialRequired'), trigger: 'change' }],
  lineNo: [{ required: true, message: t('content.validation.lineNoRequired'), trigger: 'change' }],
  hanziText: [{ required: true, message: t('content.validation.lineTextRequired'), trigger: 'blur' }]
}))

const lineVocabRules = computed<FormRules>(() => ({
  dialogueLineId: [{ required: true, message: t('content.validation.lineRequired'), trigger: 'change' }],
  wordText: [{ required: true, message: t('content.validation.wordRequired'), trigger: 'blur' }]
}))

function vocabListTreeSelectLabel(list: AdminVocabList) {
  const meta = [list.level, list.status === 'inactive' ? t('content.status.inactive') : ''].filter(Boolean)
  return meta.length ? `${list.name} / ${meta.join(' / ')}` : list.name
}

function vocabListTreeSelectFullLabel(list: AdminVocabList, recordById: Map<number, AdminVocabList>) {
  const path = buildVocabListNamePath(list, recordById)
  const meta = [list.level, list.status === 'inactive' ? t('content.status.inactive') : ''].filter(Boolean)
  return meta.length ? `${path.join(' / ')} / ${meta.join(' / ')}` : path.join(' / ')
}

function exerciseSetTreeSelectLabel(set: AdminExerciseSet) {
  const meta = [set.level, t(`content.exerciseTypes.${set.exerciseType}`), set.status === 'inactive' ? t('content.status.inactive') : ''].filter(Boolean)
  return `${set.title} / ${meta.join(' / ')}`
}

function exerciseSetTreeSelectFullLabel(set: AdminExerciseSet, recordById: Map<number, AdminExerciseSet>) {
  const path = buildExerciseSetTitlePath(set, recordById)
  const meta = [set.level, t(`content.exerciseTypes.${set.exerciseType}`), set.status === 'inactive' ? t('content.status.inactive') : ''].filter(Boolean)
  return `${path.join(' / ')} / ${meta.join(' / ')}`
}

function videoMaterialTreeSelectLabel(material: AdminVideoMaterial) {
  const meta = [t(`content.materialTypes.${material.materialType}`), material.status === 'inactive' ? t('content.status.inactive') : ''].filter(Boolean)
  return `${material.title} / ${meta.join(' / ')}`
}

function videoMaterialTreeSelectFullLabel(material: AdminVideoMaterial, recordById: Map<number, AdminVideoMaterial>) {
  const path = buildVideoMaterialTitlePath(material, recordById)
  const meta = [t(`content.materialTypes.${material.materialType}`), material.status === 'inactive' ? t('content.status.inactive') : ''].filter(Boolean)
  return `${path.join(' / ')} / ${meta.join(' / ')}`
}

function buildVocabListNamePath(list: AdminVocabList, recordById: Map<number, AdminVocabList>) {
  const path = [list.name]
  const visited = new Set<number>([list.id])
  let current = list.parentId ? recordById.get(list.parentId) : undefined
  while (current && !visited.has(current.id)) {
    path.unshift(current.name)
    visited.add(current.id)
    current = current.parentId ? recordById.get(current.parentId) : undefined
  }
  return path
}

function buildExerciseSetTitlePath(set: AdminExerciseSet, recordById: Map<number, AdminExerciseSet>) {
  const path = [set.title]
  const visited = new Set<number>([set.id])
  let current = set.parentId ? recordById.get(set.parentId) : undefined
  while (current && !visited.has(current.id)) {
    path.unshift(current.title)
    visited.add(current.id)
    current = current.parentId ? recordById.get(current.parentId) : undefined
  }
  return path
}

function buildVideoMaterialTitlePath(material: AdminVideoMaterial, recordById: Map<number, AdminVideoMaterial>) {
  const path = [material.title]
  const visited = new Set<number>([material.id])
  let current = material.parentId ? recordById.get(material.parentId) : undefined
  while (current && !visited.has(current.id)) {
    path.unshift(current.title)
    visited.add(current.id)
    current = current.parentId ? recordById.get(current.parentId) : undefined
  }
  return path
}

function buildVocabListTreeSelectOptions(records: AdminVocabList[]) {
  const recordById = new Map(records.map(record => [record.id, record]))
  const nodeById = new Map<number, ContentTreeSelectOption>()
  records.forEach(record => {
    nodeById.set(record.id, {
      value: record.id,
      label: vocabListTreeSelectFullLabel(record, recordById),
      nodeLabel: vocabListTreeSelectLabel(record)
    })
  })

  const roots: ContentTreeSelectOption[] = []
  records.forEach(record => {
    const node = nodeById.get(record.id)
    if (!node) {
      return
    }
    const parent = record.parentId ? nodeById.get(record.parentId) : null
    if (parent) {
      parent.children = parent.children || []
      parent.children.push(node)
    } else {
      roots.push(node)
    }
  })

  function sortOptions(options: ContentTreeSelectOption[]) {
    options.sort((a, b) => {
      const left = recordById.get(a.value)
      const right = recordById.get(b.value)
      return compareValue(left?.sortOrder, right?.sortOrder) || compareValue(left?.id, right?.id)
    })
    options.forEach(option => {
      if (option.children?.length) {
        sortOptions(option.children)
      } else {
        delete option.children
      }
    })
  }

  sortOptions(roots)
  return roots
}

function buildExerciseSetTreeSelectOptions(records: AdminExerciseSet[]) {
  const recordById = new Map(records.map(record => [record.id, record]))
  const nodeById = new Map<number, ContentTreeSelectOption>()
  records.forEach(record => {
    nodeById.set(record.id, {
      value: record.id,
      label: exerciseSetTreeSelectFullLabel(record, recordById),
      nodeLabel: exerciseSetTreeSelectLabel(record)
    })
  })

  const roots: ContentTreeSelectOption[] = []
  records.forEach(record => {
    const node = nodeById.get(record.id)
    if (!node) {
      return
    }
    const parent = record.parentId ? nodeById.get(record.parentId) : null
    if (parent) {
      parent.children = parent.children || []
      parent.children.push(node)
    } else {
      roots.push(node)
    }
  })

  function sortOptions(options: ContentTreeSelectOption[]) {
    options.sort((a, b) => compareValue(recordById.get(a.value)?.id, recordById.get(b.value)?.id))
    options.forEach(option => {
      if (option.children?.length) {
        sortOptions(option.children)
      } else {
        delete option.children
      }
    })
  }

  sortOptions(roots)
  return roots
}

function buildVideoMaterialTreeSelectOptions(records: AdminVideoMaterial[]) {
  const recordById = new Map(records.map(record => [record.id, record]))
  const nodeById = new Map<number, ContentTreeSelectOption>()
  records.forEach(record => {
    nodeById.set(record.id, {
      value: record.id,
      label: videoMaterialTreeSelectFullLabel(record, recordById),
      nodeLabel: videoMaterialTreeSelectLabel(record)
    })
  })

  const roots: ContentTreeSelectOption[] = []
  records.forEach(record => {
    const node = nodeById.get(record.id)
    if (!node) {
      return
    }
    const parent = record.parentId ? nodeById.get(record.parentId) : null
    if (parent) {
      parent.children = parent.children || []
      parent.children.push(node)
    } else {
      roots.push(node)
    }
  })

  function sortOptions(options: ContentTreeSelectOption[]) {
    options.sort((a, b) => compareValue(recordById.get(a.value)?.id, recordById.get(b.value)?.id))
    options.forEach(option => {
      if (option.children?.length) {
        sortOptions(option.children)
      } else {
        delete option.children
      }
    })
  }

  sortOptions(roots)
  return roots
}

function isVocabListInSubtree(candidateId: number, rootId?: number | null) {
  if (!rootId) {
    return false
  }
  const visited = new Set<number>()
  let current = findVocabList(candidateId)
  while (current) {
    if (current.id === rootId) {
      return true
    }
    if (!current.parentId || visited.has(current.id)) {
      return false
    }
    visited.add(current.id)
    current = findVocabList(current.parentId)
  }
  return false
}

function isExerciseSetInSubtree(candidateId: number, rootId?: number | null) {
  if (!rootId) {
    return false
  }
  const visited = new Set<number>()
  let current = findExerciseSet(candidateId)
  while (current) {
    if (current.id === rootId) {
      return true
    }
    if (!current.parentId || visited.has(current.id)) {
      return false
    }
    visited.add(current.id)
    current = findExerciseSet(current.parentId)
  }
  return false
}

function isVideoMaterialInSubtree(candidateId: number, rootId?: number | null) {
  if (!rootId) {
    return false
  }
  const visited = new Set<number>()
  let current = findVideoMaterial(candidateId)
  while (current) {
    if (current.id === rootId) {
      return true
    }
    if (!current.parentId || visited.has(current.id)) {
      return false
    }
    visited.add(current.id)
    current = findVideoMaterial(current.parentId)
  }
  return false
}

function toVocabListTreeNode(record: AdminVocabList): AdminVocabListTreeNode {
  return { ...record }
}

function toExerciseSetTreeNode(record: AdminExerciseSet): AdminExerciseSetTreeNode {
  return { ...record }
}

function toVideoMaterialTreeNode(record: AdminVideoMaterial): AdminVideoMaterialTreeNode {
  return { ...record }
}

function compareValue(a: string | number | null | undefined, b: string | number | null | undefined) {
  if (a === b) {
    return 0
  }
  if (a === null || a === undefined || a === '') {
    return 1
  }
  if (b === null || b === undefined || b === '') {
    return -1
  }
  if (typeof a === 'number' && typeof b === 'number') {
    return a - b
  }
  return String(a).localeCompare(String(b), locale.value)
}

function compareVocabLists(a: AdminVocabListTreeNode, b: AdminVocabListTreeNode) {
  const direction = listQuery.sortDirection === 'desc' ? -1 : 1
  if (listQuery.sortBy === 'id') {
    return direction * compareValue(a.id, b.id)
  }
  if (listQuery.sortBy === 'name') {
    return direction * compareValue(a.name, b.name)
  }
  if (listQuery.sortBy === 'listType') {
    return direction * compareValue(a.listType, b.listType)
  }
  if (listQuery.sortBy === 'level') {
    return direction * compareValue(a.level, b.level)
  }
  if (listQuery.sortBy === 'sortOrder') {
    return direction * compareValue(a.sortOrder, b.sortOrder)
  }
  if (listQuery.sortBy === 'status') {
    return direction * compareValue(a.status, b.status)
  }
  if (listQuery.sortBy === 'updatedAt') {
    return direction * compareValue(a.updatedAt, b.updatedAt)
  }
  if (listQuery.sortBy === 'createdAt') {
    return direction * compareValue(a.createdAt, b.createdAt)
  }
  return compareValue(a.sortOrder, b.sortOrder) || compareValue(a.id, b.id)
}

function compareExerciseSets(a: AdminExerciseSetTreeNode, b: AdminExerciseSetTreeNode) {
  const direction = setQuery.sortDirection === 'desc' ? -1 : 1
  if (setQuery.sortBy === 'id') {
    return direction * compareValue(a.id, b.id)
  }
  if (setQuery.sortBy === 'title') {
    return direction * compareValue(a.title, b.title)
  }
  if (setQuery.sortBy === 'exerciseType') {
    return direction * compareValue(a.exerciseType, b.exerciseType)
  }
  if (setQuery.sortBy === 'level') {
    return direction * compareValue(a.level, b.level)
  }
  if (setQuery.sortBy === 'status') {
    return direction * compareValue(a.status, b.status)
  }
  if (setQuery.sortBy === 'updatedAt') {
    return direction * compareValue(a.updatedAt, b.updatedAt)
  }
  if (setQuery.sortBy === 'createdAt') {
    return direction * compareValue(a.createdAt, b.createdAt)
  }
  return compareValue(a.id, b.id)
}

function compareVideoMaterials(a: AdminVideoMaterialTreeNode, b: AdminVideoMaterialTreeNode) {
  const direction = materialQuery.sortDirection === 'desc' ? -1 : 1
  if (materialQuery.sortBy === 'id') {
    return direction * compareValue(a.id, b.id)
  }
  if (materialQuery.sortBy === 'title') {
    return direction * compareValue(a.title, b.title)
  }
  if (materialQuery.sortBy === 'materialType') {
    return direction * compareValue(a.materialType, b.materialType)
  }
  if (materialQuery.sortBy === 'status') {
    return direction * compareValue(a.status, b.status)
  }
  if (materialQuery.sortBy === 'updatedAt') {
    return direction * compareValue(a.updatedAt, b.updatedAt)
  }
  if (materialQuery.sortBy === 'createdAt') {
    return direction * compareValue(a.createdAt, b.createdAt)
  }
  return compareValue(a.id, b.id)
}

function buildVocabListTree(records: AdminVocabList[]) {
  const nodes = new Map<number, AdminVocabListTreeNode>()
  const sourceById = new Map(listOptions.value.map(item => [item.id, item]))

  function ensureNode(record: AdminVocabList) {
    if (!nodes.has(record.id)) {
      nodes.set(record.id, toVocabListTreeNode(record))
    }
    return nodes.get(record.id)!
  }

  records.forEach(record => {
    let current: AdminVocabList | undefined = record
    while (current) {
      ensureNode(current)
      current = current.parentId ? sourceById.get(current.parentId) : undefined
    }
  })

  const roots: AdminVocabListTreeNode[] = []
  nodes.forEach(node => {
    const parent = node.parentId ? nodes.get(node.parentId) : null
    if (parent) {
      parent.children = parent.children || []
      parent.children.push(node)
    } else {
      roots.push(node)
    }
  })

  function sortTree(items: AdminVocabListTreeNode[]) {
    items.sort(compareVocabLists)
    items.forEach(item => {
      if (item.children?.length) {
        sortTree(item.children)
      } else {
        delete item.children
      }
    })
  }

  sortTree(roots)
  return roots
}

function buildExerciseSetTree(records: AdminExerciseSet[]) {
  const nodes = new Map<number, AdminExerciseSetTreeNode>()
  const sourceById = new Map(setOptions.value.map(item => [item.id, item]))

  function ensureNode(record: AdminExerciseSet) {
    if (!nodes.has(record.id)) {
      nodes.set(record.id, toExerciseSetTreeNode(record))
    }
    return nodes.get(record.id)!
  }

  records.forEach(record => {
    let current: AdminExerciseSet | undefined = record
    while (current) {
      ensureNode(current)
      current = current.parentId ? sourceById.get(current.parentId) : undefined
    }
  })

  const roots: AdminExerciseSetTreeNode[] = []
  nodes.forEach(node => {
    const parent = node.parentId ? nodes.get(node.parentId) : null
    if (parent) {
      parent.children = parent.children || []
      parent.children.push(node)
    } else {
      roots.push(node)
    }
  })

  function sortTree(items: AdminExerciseSetTreeNode[]) {
    items.sort(compareExerciseSets)
    items.forEach(item => {
      if (item.children?.length) {
        sortTree(item.children)
      } else {
        delete item.children
      }
    })
  }

  sortTree(roots)
  return roots
}

function buildVideoMaterialTree(records: AdminVideoMaterial[]) {
  const nodes = new Map<number, AdminVideoMaterialTreeNode>()
  const sourceById = new Map(materialOptions.value.map(item => [item.id, item]))

  function ensureNode(record: AdminVideoMaterial) {
    if (!nodes.has(record.id)) {
      nodes.set(record.id, toVideoMaterialTreeNode(record))
    }
    return nodes.get(record.id)!
  }

  records.forEach(record => {
    let current: AdminVideoMaterial | undefined = record
    while (current) {
      ensureNode(current)
      current = current.parentId ? sourceById.get(current.parentId) : undefined
    }
  })

  const roots: AdminVideoMaterialTreeNode[] = []
  nodes.forEach(node => {
    const parent = node.parentId ? nodes.get(node.parentId) : null
    if (parent) {
      parent.children = parent.children || []
      parent.children.push(node)
    } else {
      roots.push(node)
    }
  })

  function sortTree(items: AdminVideoMaterialTreeNode[]) {
    items.sort(compareVideoMaterials)
    items.forEach(item => {
      if (item.children?.length) {
        sortTree(item.children)
      } else {
        delete item.children
      }
    })
  }

  sortTree(roots)
  return roots
}

function paginateTree<T>(records: T[], query: { page: number; pageSize: number }) {
  const maxPage = Math.max(1, Math.ceil(records.length / query.pageSize))
  if (query.page > maxPage) {
    query.page = maxPage
  }
  const start = (query.page - 1) * query.pageSize
  return records.slice(start, start + query.pageSize)
}

async function fetchAllVocabListsForTree() {
  const records: AdminVocabList[] = []
  let page = 1
  let total = 0
  do {
    const result = await fetchAdminVocabLists({
      page,
      pageSize: treeChildPageSize,
      keyword: listQuery.keyword,
      listType: listQuery.listType,
      level: listQuery.level,
      status: listQuery.status,
      sortBy: listQuery.sortBy,
      sortDirection: listQuery.sortDirection
    })
    records.push(...result.records)
    total = result.total
    page += 1
  } while (records.length < total)
  return listQuery.status === 'active' ? records.filter(record => !isVocabListHierarchyInactive(record.id)) : records
}

async function fetchAllExerciseSetsForTree() {
  const records: AdminExerciseSet[] = []
  let page = 1
  let total = 0
  do {
    const result = await fetchAdminExerciseSets({
      page,
      pageSize: treeChildPageSize,
      keyword: setQuery.keyword,
      exerciseType: setQuery.exerciseType,
      level: setQuery.level,
      status: setQuery.status,
      sortBy: setQuery.sortBy,
      sortDirection: setQuery.sortDirection
    })
    records.push(...result.records)
    total = result.total
    page += 1
  } while (records.length < total)
  return setQuery.status === 'active' ? records.filter(record => !isExerciseSetHierarchyInactive(record.id)) : records
}

async function fetchAllVideoMaterialsForTree() {
  const records: AdminVideoMaterial[] = []
  let page = 1
  let total = 0
  do {
    const result = await fetchAdminVideoMaterials({
      page,
      pageSize: treeChildPageSize,
      keyword: materialQuery.keyword,
      materialType: materialQuery.materialType,
      status: materialQuery.status,
      hasCover: materialQuery.hasCover,
      sortBy: materialQuery.sortBy,
      sortDirection: materialQuery.sortDirection
    })
    records.push(...result.records)
    total = result.total
    page += 1
  } while (records.length < total)
  return materialQuery.status === 'active' ? records.filter(record => !isVideoMaterialHierarchyInactive(record.id)) : records
}

async function loadLists() {
  listLoading.value = true
  try {
    const tree = buildVocabListTree(await fetchAllVocabListsForTree())
    listTotal.value = tree.length
    vocabLists.value = paginateTree(tree, listQuery)
  } finally {
    listLoading.value = false
  }
}

async function loadListOptions() {
  const records: AdminVocabList[] = []
  let page = 1
  let total = 0
  do {
    const result = await fetchAdminVocabLists({ page, pageSize: treeChildPageSize, keyword: '', listType: '', level: '', status: '' })
    records.push(...result.records)
    total = result.total
    page += 1
  } while (records.length < total)
  listOptions.value = records
}

async function loadItems() {
  itemLoading.value = true
  try {
    const result = await fetchAdminVocabItems(itemQuery)
    vocabItems.value = result.records
    itemTotal.value = result.total
    appliedItemVocabListId.value = itemQuery.vocabListId || null
  } finally {
    itemLoading.value = false
  }
}

async function loadVocabItemOptions() {
  const result = await fetchAdminVocabItems({ page: 1, pageSize: 100, vocabListId: null, keyword: '', status: '' })
  vocabItemOptions.value = result.records
}

async function loadMediaAssets() {
  mediaLoading.value = true
  try {
    const result = await fetchAdminMediaAssets(mediaQuery)
    mediaAssets.value = result.records
    mediaTotal.value = result.total
  } finally {
    mediaLoading.value = false
  }
}

async function loadAudioOptions() {
  const result = await fetchAdminMediaAssets({ page: 1, pageSize: 100, keyword: '', mediaType: 'audio', language: '', status: 'active' })
  audioOptions.value = result.records
}

async function loadImageOptions() {
  const result = await fetchAdminMediaAssets({ page: 1, pageSize: 100, keyword: '', mediaType: 'image', language: '', status: 'active' })
  imageOptions.value = result.records
}

async function loadExerciseSets() {
  setLoading.value = true
  try {
    const tree = buildExerciseSetTree(await fetchAllExerciseSetsForTree())
    setTotal.value = tree.length
    exerciseSets.value = paginateTree(tree, setQuery)
  } finally {
    setLoading.value = false
  }
}

async function loadExerciseSetOptions() {
  const records: AdminExerciseSet[] = []
  let page = 1
  let total = 0
  do {
    const result = await fetchAdminExerciseSets({ page, pageSize: treeChildPageSize, keyword: '', exerciseType: '', level: '', status: '' })
    records.push(...result.records)
    total = result.total
    page += 1
  } while (records.length < total)
  setOptions.value = records
}

async function loadSentenceExercises() {
  exerciseLoading.value = true
  try {
    const result = await fetchAdminSentenceExercises(exerciseQuery)
    sentenceExercises.value = result.records
    exerciseTotal.value = result.total
    appliedExerciseSetId.value = exerciseQuery.exerciseSetId || null
  } finally {
    exerciseLoading.value = false
  }
}

async function loadVideoMaterials() {
  materialLoading.value = true
  try {
    if (materialOptions.value.length === 0) {
      await loadMaterialOptions()
    }
    const tree = buildVideoMaterialTree(await fetchAllVideoMaterialsForTree())
    materialTotal.value = tree.length
    videoMaterials.value = paginateTree(tree, materialQuery)
  } finally {
    materialLoading.value = false
  }
}

async function loadMaterialOptions() {
  const records: AdminVideoMaterial[] = []
  let page = 1
  let total = 0
  do {
    const result = await fetchAdminVideoMaterials({ page, pageSize: treeChildPageSize, keyword: '', materialType: '', status: '' })
    records.push(...result.records)
    total = result.total
    page += 1
  } while (records.length < total)
  materialOptions.value = records
}

async function loadDialogueLines() {
  lineLoading.value = true
  try {
    const result = await fetchAdminDialogueLines(lineQuery)
    dialogueLines.value = result.records
    lineTotal.value = result.total
    appliedLineMaterialId.value = lineQuery.materialId || null
  } finally {
    lineLoading.value = false
  }
}

async function loadLineOptions(materialId?: number | null) {
  const result = await fetchAdminDialogueLines({ page: 1, pageSize: 100, materialId: materialId || null, keyword: '' })
  lineOptions.value = result.records
}

async function loadLineVocab() {
  lineVocabLoading.value = true
  try {
    const result = await fetchAdminDialogueLineVocab(lineVocabQuery)
    lineVocabRecords.value = result.records
    lineVocabTotal.value = result.total
    appliedLineVocabMaterialId.value = lineVocabQuery.materialId || null
    appliedLineVocabDialogueLineId.value = lineVocabQuery.dialogueLineId || null
  } finally {
    lineVocabLoading.value = false
  }
}

function reloadActive() {
  if (activeTab.value === 'lists') {
    void loadLists()
  } else if (activeTab.value === 'items') {
    void loadItems()
  } else if (activeTab.value === 'media') {
    void loadMediaAssets()
  } else if (activeTab.value === 'sets') {
    void loadExerciseSets()
  } else if (activeTab.value === 'exercises') {
    void loadSentenceExercises()
  } else if (activeTab.value === 'materials') {
    void loadVideoMaterials()
  } else if (activeTab.value === 'lines') {
    void loadDialogueLines()
  } else {
    void loadLineVocab()
  }
}

function handleTabChange() {
  if (activeTab.value === 'items') {
    void loadListOptions()
    void loadAudioOptions()
  }
  if (activeTab.value === 'exercises') {
    void loadExerciseSetOptions()
    void loadAudioOptions()
  }
  if (activeTab.value === 'materials') {
    void loadMaterialOptions()
    void loadImageOptions()
  }
  if (activeTab.value === 'lines') {
    void loadMaterialOptions()
    void loadAudioOptions()
  }
  if (activeTab.value === 'lineVocab') {
    void loadMaterialOptions()
    void loadLineOptions(lineVocabQuery.materialId)
    void loadVocabItemOptions()
  }
  reloadActive()
}

function isInactiveStatus(status?: ContentStatus | null) {
  return status === 'inactive'
}

function findVocabList(listId?: number | null) {
  return listOptions.value.find(item => item.id === listId) || null
}

function findExerciseSet(setId?: number | null) {
  return setOptions.value.find(item => item.id === setId) || null
}

function isVocabListHierarchyInactive(listId?: number | null) {
  if (!listId) {
    return false
  }
  const visited = new Set<number>()
  let current = findVocabList(listId)
  while (current) {
    if (isInactiveStatus(current.status)) {
      return true
    }
    if (!current.parentId || visited.has(current.id)) {
      return false
    }
    visited.add(current.id)
    current = findVocabList(current.parentId)
  }
  return false
}

function isExerciseSetHierarchyInactive(setId?: number | null) {
  if (!setId) {
    return false
  }
  const visited = new Set<number>()
  let current = findExerciseSet(setId)
  while (current) {
    if (isInactiveStatus(current.status)) {
      return true
    }
    if (!current.parentId || visited.has(current.id)) {
      return false
    }
    visited.add(current.id)
    current = findExerciseSet(current.parentId)
  }
  return false
}

function findVideoMaterial(materialId?: number | null) {
  return materialOptions.value.find(item => item.id === materialId) || null
}

function isVideoMaterialHierarchyInactive(materialId?: number | null) {
  if (!materialId) {
    return false
  }
  const visited = new Set<number>()
  let current = findVideoMaterial(materialId)
  while (current) {
    if (isInactiveStatus(current.status)) {
      return true
    }
    if (!current.parentId || visited.has(current.id)) {
      return false
    }
    visited.add(current.id)
    current = findVideoMaterial(current.parentId)
  }
  return false
}

function findDialogueLine(lineId?: number | null) {
  return lineOptions.value.find(item => item.id === lineId) || dialogueLines.value.find(item => item.id === lineId) || null
}

function isVocabItemReadonly(item: AdminVocabItem) {
  return !item
}

function isSentenceExerciseReadonly(exercise: AdminSentenceExercise) {
  return !exercise
}

function isDialogueLineReadonly(line: AdminDialogueLine) {
  return isVideoMaterialHierarchyInactive(line.materialId) || isInactiveStatus(line.materialStatus)
}

function isLineVocabReadonly(record: AdminDialogueLineVocab) {
  return isVideoMaterialHierarchyInactive(record.materialId) || isInactiveStatus(record.materialStatus)
}

function isImportContextInactive(importType: AdminContentImportType, contextId: number | null) {
  if (!contextId) {
    return false
  }
  if (importType === 'vocab-items') {
    return false
  }
  if (importType === 'sentence-exercises') {
    return false
  }
  if (importType === 'dialogue-lines') {
    return isVideoMaterialHierarchyInactive(contextId)
  }
  if (importType === 'dialogue-line-vocab') {
    const line = findDialogueLine(contextId)
    return isVideoMaterialHierarchyInactive(line?.materialId)
  }
  return false
}

function bulkBindTargetReadonly(target: BulkBindTarget) {
  if (target === 'itemAudio') {
    return vocabItemsReadonly.value
  }
  if (target === 'exerciseAudio') {
    return sentenceExercisesReadonly.value
  }
  if (target === 'lineAudio') {
    return dialogueLinesReadonly.value
  }
  return false
}

function searchLists() {
  listQuery.page = 1
  void loadLists()
}

function resetListFilters() {
  listQuery.keyword = ''
  listQuery.listType = ''
  listQuery.level = ''
  listQuery.status = defaultContentStatus
  listQuery.page = 1
  void loadLists()
}

function handleListPageSizeChange() {
  listQuery.page = 1
  void loadLists()
}

function handleListSortChange(event: TableSortChange) {
  applyTableSort(listQuery, event)
  void loadLists()
}

function searchItems() {
  itemQuery.page = 1
  void loadItems()
}

function resetItemFilters() {
  itemQuery.keyword = ''
  itemQuery.vocabListId = null
  itemQuery.status = defaultContentStatus
  itemQuery.hasAudio = null
  itemQuery.page = 1
  void loadItems()
}

function handleItemPageSizeChange() {
  itemQuery.page = 1
  void loadItems()
}

function handleItemSortChange(event: TableSortChange) {
  applyTableSort(itemQuery, event)
  void loadItems()
}

function searchMedia() {
  mediaQuery.page = 1
  void loadMediaAssets()
}

function resetMediaFilters() {
  mediaQuery.keyword = ''
  mediaQuery.mediaType = 'audio'
  mediaQuery.language = ''
  mediaQuery.status = defaultContentStatus
  mediaQuery.page = 1
  void loadMediaAssets()
}

function handleMediaPageSizeChange() {
  mediaQuery.page = 1
  void loadMediaAssets()
}

function handleMediaSortChange(event: TableSortChange) {
  applyTableSort(mediaQuery, event)
  void loadMediaAssets()
}

function searchSets() {
  setQuery.page = 1
  void loadExerciseSets()
}

function resetSetFilters() {
  setQuery.keyword = ''
  setQuery.exerciseType = ''
  setQuery.level = ''
  setQuery.status = defaultContentStatus
  setQuery.page = 1
  void loadExerciseSets()
}

function handleSetPageSizeChange() {
  setQuery.page = 1
  void loadExerciseSets()
}

function handleSetSortChange(event: TableSortChange) {
  applyTableSort(setQuery, event)
  void loadExerciseSets()
}

function searchExercises() {
  exerciseQuery.page = 1
  void loadSentenceExercises()
}

function resetExerciseFilters() {
  exerciseQuery.keyword = ''
  exerciseQuery.exerciseSetId = null
  exerciseQuery.exerciseType = ''
  exerciseQuery.status = defaultContentStatus
  exerciseQuery.hasAudio = null
  exerciseQuery.page = 1
  void loadSentenceExercises()
}

function handleExercisePageSizeChange() {
  exerciseQuery.page = 1
  void loadSentenceExercises()
}

function handleExerciseSortChange(event: TableSortChange) {
  applyTableSort(exerciseQuery, event)
  void loadSentenceExercises()
}

function searchMaterials() {
  materialQuery.page = 1
  void loadVideoMaterials()
}

function resetMaterialFilters() {
  materialQuery.keyword = ''
  materialQuery.materialType = ''
  materialQuery.status = defaultContentStatus
  materialQuery.hasCover = null
  materialQuery.page = 1
  void loadVideoMaterials()
}

function handleMaterialPageSizeChange() {
  materialQuery.page = 1
  void loadVideoMaterials()
}

function handleMaterialSortChange(event: TableSortChange) {
  applyTableSort(materialQuery, event)
  void loadVideoMaterials()
}

function searchLines() {
  lineQuery.page = 1
  void loadDialogueLines()
}

function resetLineFilters() {
  lineQuery.keyword = ''
  lineQuery.materialId = null
  lineQuery.hasAudio = null
  lineQuery.page = 1
  void loadDialogueLines()
}

function handleLinePageSizeChange() {
  lineQuery.page = 1
  void loadDialogueLines()
}

function handleLineSortChange(event: TableSortChange) {
  applyTableSort(lineQuery, event)
  void loadDialogueLines()
}

function searchLineVocab() {
  lineVocabQuery.page = 1
  void loadLineVocab()
}

function resetLineVocabFilters() {
  lineVocabQuery.keyword = ''
  lineVocabQuery.materialId = null
  lineVocabQuery.dialogueLineId = null
  lineVocabQuery.page = 1
  void loadLineOptions(null)
  void loadLineVocab()
}

function handleLineVocabPageSizeChange() {
  lineVocabQuery.page = 1
  void loadLineVocab()
}

function handleLineVocabSortChange(event: TableSortChange) {
  applyTableSort(lineVocabQuery, event)
  void loadLineVocab()
}

function handleLineVocabMaterialFilterChange(materialId: number | null) {
  lineVocabQuery.dialogueLineId = null
  lineVocabQuery.page = 1
  void loadLineOptions(materialId)
}

function filterItemsByList(list: AdminVocabList) {
  activeTab.value = 'items'
  itemQuery.vocabListId = list.id
  itemQuery.page = 1
  void loadListOptions()
  void loadItems()
}

function filterExercisesBySet(set: AdminExerciseSet) {
  activeTab.value = 'exercises'
  exerciseQuery.exerciseSetId = set.id
  exerciseQuery.exerciseType = ''
  exerciseQuery.page = 1
  void loadExerciseSetOptions()
  void loadSentenceExercises()
}

function filterLinesByMaterial(material: AdminVideoMaterial) {
  activeTab.value = 'lines'
  lineQuery.materialId = material.id
  lineQuery.page = 1
  void loadMaterialOptions()
  void loadDialogueLines()
}

function filterVocabByLine(line: AdminDialogueLine) {
  activeTab.value = 'lineVocab'
  lineVocabQuery.materialId = line.materialId
  lineVocabQuery.dialogueLineId = line.id
  lineVocabQuery.page = 1
  void loadMaterialOptions()
  void loadLineOptions(line.materialId)
  void loadVocabItemOptions()
  void loadLineVocab()
}

function openListDialog(list?: AdminVocabList) {
  editingList.value = list || null
  listForm.name = list?.name || ''
  listForm.parentId = list?.parentId || null
  listForm.listType = list?.listType || 'HSK'
  listForm.level = list?.level || ''
  listForm.description = list?.description || ''
  listForm.sortOrder = list?.sortOrder || 0
  listForm.status = list?.status || 'active'
  listDialogVisible.value = true
  if (listOptions.value.length === 0) {
    void loadListOptions()
  }
}

function openItemDialog(item?: AdminVocabItem) {
  editingItem.value = item || null
  const initialListIds = item?.vocabListIds?.length
    ? item.vocabListIds
    : item?.vocabListId
      ? [item.vocabListId]
      : appliedItemVocabListId.value || itemQuery.vocabListId
        ? [appliedItemVocabListId.value || itemQuery.vocabListId].filter(Boolean) as number[]
        : []
  itemForm.vocabListIds = [...initialListIds]
  itemForm.vocabListId = initialListIds[0] || null
  itemForm.hanzi = item?.hanzi || ''
  itemForm.pinyin = item?.pinyin || ''
  itemForm.meaningEn = item?.meaningEn || ''
  itemForm.meaningRu = item?.meaningRu || ''
  itemForm.exampleSentence = item?.exampleSentence || ''
  itemForm.audioAssetId = item?.audioAssetId || null
  itemForm.sortOrder = item?.sortOrder || 0
  itemForm.status = item?.status || 'active'
  itemDialogVisible.value = true
  if (listOptions.value.length === 0) {
    void loadListOptions()
  }
  if (audioOptions.value.length === 0) {
    void loadAudioOptions()
  }
}

function openSetDialog(set?: AdminExerciseSet) {
  editingSet.value = set || null
  setForm.title = set?.title || ''
  setForm.parentId = set?.parentId || null
  setForm.exerciseType = set?.exerciseType || 'audio_order'
  setForm.level = set?.level || ''
  setForm.status = set?.status || 'active'
  setDialogVisible.value = true
  if (setOptions.value.length === 0) {
    void loadExerciseSetOptions()
  }
}

function openExerciseDialog(exercise?: AdminSentenceExercise) {
  editingExercise.value = exercise || null
  const initialSetIds = exercise
    ? exercise.exerciseSetIds?.length
      ? exercise.exerciseSetIds
      : exercise.exerciseSetId
        ? [exercise.exerciseSetId]
        : []
    : []
  const initialSetId = initialSetIds[0] || null
  exerciseForm.exerciseSetIds = [...initialSetIds]
  exerciseForm.exerciseSetId = initialSetId
  exerciseForm.exerciseType = exercise?.exerciseType || 'audio_order'
  exerciseForm.hanziAnswer = exercise?.hanziAnswer || ''
  exerciseForm.pinyinPrompt = exercise?.pinyinPrompt || ''
  exerciseForm.translationEn = exercise?.translationEn || ''
  exerciseForm.translationRu = exercise?.translationRu || ''
  exerciseForm.audioZhAssetId = exercise?.audioZhAssetId || null
  exerciseForm.explanation = exercise?.explanation || ''
  exerciseForm.sortOrder = exercise?.sortOrder || 0
  exerciseForm.status = exercise?.status || 'active'
  exerciseForm.wordOptionsText = wordOptionsToText(exercise)
  exerciseDialogVisible.value = true
  if (setOptions.value.length === 0) {
    void loadExerciseSetOptions()
  }
  if (audioOptions.value.length === 0) {
    void loadAudioOptions()
  }
}

function handleExerciseSetChange(setIds?: number[] | number | null) {
  const ids = Array.isArray(setIds) ? setIds : setIds ? [setIds] : []
  exerciseForm.exerciseSetId = ids[0] || null
}

function handleMaterialParentChange(parentId?: number | null) {
  const selected = findVideoMaterial(parentId)
  if (selected) {
    materialForm.materialType = selected.materialType
  }
}

function handleMaterialTypeChange(materialType: VideoMaterialType) {
  const parent = findVideoMaterial(materialForm.parentId)
  if (parent && parent.materialType !== materialType) {
    materialForm.parentId = null
  }
}

function openMaterialDialog(material?: AdminVideoMaterial) {
  editingMaterial.value = material || null
  materialForm.title = material?.title || ''
  materialForm.parentId = material?.parentId || null
  materialForm.materialType = material?.materialType || 'drama'
  materialForm.description = material?.description || ''
  materialForm.coverAssetId = material?.coverAssetId || null
  materialForm.status = material?.status || 'active'
  materialDialogVisible.value = true
  if (materialOptions.value.length === 0) {
    void loadMaterialOptions()
  }
  if (imageOptions.value.length === 0) {
    void loadImageOptions()
  }
}

function openLineDialog(line?: AdminDialogueLine) {
  editingLine.value = line || null
  lineForm.materialId = line?.materialId || appliedLineMaterialId.value || lineQuery.materialId || materialOptions.value[0]?.id || null
  lineForm.lineNo = line?.lineNo || 1
  lineForm.hanziText = line?.hanziText || ''
  lineForm.pinyinText = line?.pinyinText || ''
  lineForm.translationEn = line?.translationEn || ''
  lineForm.translationRu = line?.translationRu || ''
  lineForm.audioAssetId = line?.audioAssetId || null
  lineForm.startMs = line?.startMs ?? null
  lineForm.endMs = line?.endMs ?? null
  lineDialogVisible.value = true
  if (materialOptions.value.length === 0) {
    void loadMaterialOptions()
  }
  if (audioOptions.value.length === 0) {
    void loadAudioOptions()
  }
}

function openLineVocabDialog(record?: AdminDialogueLineVocab) {
  editingLineVocab.value = record || null
  lineVocabForm.dialogueLineId = record?.dialogueLineId || appliedLineVocabDialogueLineId.value || lineVocabQuery.dialogueLineId || lineOptions.value[0]?.id || null
  lineVocabForm.vocabItemId = record?.vocabItemId || null
  lineVocabForm.wordText = record?.wordText || ''
  lineVocabForm.pinyin = record?.pinyin || ''
  lineVocabForm.meaningEn = record?.meaningEn || ''
  lineVocabForm.meaningRu = record?.meaningRu || ''
  lineVocabForm.explanation = record?.explanation || ''
  lineVocabDialogVisible.value = true
  if (lineOptions.value.length === 0) {
    void loadLineOptions(lineVocabQuery.materialId)
  }
  if (vocabItemOptions.value.length === 0) {
    void loadVocabItemOptions()
  }
}

function handleLineVocabItemChange(itemId: number | null) {
  const item = vocabItemOptions.value.find(option => option.id === itemId)
  if (!item) {
    return
  }
  if (!lineVocabForm.wordText) {
    lineVocabForm.wordText = item.hanzi
  }
  if (!lineVocabForm.pinyin) {
    lineVocabForm.pinyin = item.pinyin || ''
  }
  if (!lineVocabForm.meaningEn) {
    lineVocabForm.meaningEn = item.meaningEn || ''
  }
  if (!lineVocabForm.meaningRu) {
    lineVocabForm.meaningRu = item.meaningRu || ''
  }
}

function openUploadDialog(target: UploadTarget = 'general', mediaType: MediaType = 'audio') {
  uploadTarget.value = target
  uploadForm.mediaType = mediaType
  uploadForm.language = mediaType === 'audio' ? 'zh' : ''
  uploadForm.durationMs = null
  uploadForm.fileList = []
  uploadDialogVisible.value = true
}

function openBulkBindDialog(target: BulkBindTarget) {
  if (bulkBindTargetReadonly(target)) {
    ElMessage.warning(t('content.parentReadonly.submitDisabled'))
    return
  }
  bulkBindForm.target = target
  bulkBindForm.rowsText = ''
  bulkBindDialogVisible.value = true
  if (target === 'materialCover') {
    void loadImageOptions()
    return
  }
  void loadAudioOptions()
}

function openCsvImportDialog(importType: AdminContentImportType) {
  csvImportForm.importType = importType
  csvImportForm.contextId = defaultImportContextId(importType)
  csvImportForm.contextIds = defaultImportContextIds(importType)
  csvImportForm.fileList = []
  clearCsvImportError()
  csvImportDialogVisible.value = true
  void prepareImportContextOptions(importType)
}

async function submitList() {
  await listFormRef.value?.validate()
  submitting.value = true
  try {
    const payload = {
      name: listForm.name.trim(),
      parentId: listForm.parentId,
      listType: listForm.listType,
      level: blankToNull(listForm.level),
      description: blankToNull(listForm.description),
      sortOrder: listForm.sortOrder,
      status: listForm.status
    }
    if (editingList.value) {
      await updateAdminVocabList(editingList.value.id, payload)
    } else {
      await createAdminVocabList(payload)
    }
    listDialogVisible.value = false
    ElMessage.success(t('content.saved'))
    await loadListOptions()
    await loadLists()
  } finally {
    submitting.value = false
  }
}

async function submitItem() {
  await itemFormRef.value?.validate()
  if (itemFormReadonly.value) {
    ElMessage.warning(t('content.parentReadonly.submitDisabled'))
    return
  }
  submitting.value = true
  try {
    itemForm.vocabListId = itemForm.vocabListIds[0] || null
    const payload = {
      vocabListId: itemForm.vocabListId,
      vocabListIds: itemForm.vocabListIds,
      hanzi: itemForm.hanzi.trim(),
      pinyin: blankToNull(itemForm.pinyin),
      meaningEn: blankToNull(itemForm.meaningEn),
      meaningRu: blankToNull(itemForm.meaningRu),
      exampleSentence: blankToNull(itemForm.exampleSentence),
      audioAssetId: itemForm.audioAssetId,
      sortOrder: itemForm.sortOrder,
      status: itemForm.status
    }
    if (editingItem.value) {
      await updateAdminVocabItem(editingItem.value.id, payload)
    } else {
      await createAdminVocabItem(payload)
    }
    itemDialogVisible.value = false
    ElMessage.success(t('content.saved'))
    await loadItems()
  } finally {
    submitting.value = false
  }
}

async function submitSet() {
  await setFormRef.value?.validate()
  submitting.value = true
  try {
    const payload = {
      title: setForm.title.trim(),
      parentId: setForm.parentId,
      exerciseType: setForm.exerciseType,
      level: blankToNull(setForm.level),
      status: setForm.status
    }
    if (editingSet.value) {
      await updateAdminExerciseSet(editingSet.value.id, payload)
    } else {
      await createAdminExerciseSet(payload)
    }
    setDialogVisible.value = false
    ElMessage.success(t('content.saved'))
    await loadExerciseSetOptions()
    await loadExerciseSets()
  } finally {
    submitting.value = false
  }
}

async function submitExercise() {
  await exerciseFormRef.value?.validate()
  if (exerciseFormReadonly.value) {
    ElMessage.warning(t('content.parentReadonly.submitDisabled'))
    return
  }
  submitting.value = true
  try {
    exerciseForm.exerciseSetId = exerciseForm.exerciseSetIds[0] || null
    const payload = {
      exerciseSetId: exerciseForm.exerciseSetId,
      exerciseSetIds: exerciseForm.exerciseSetIds,
      exerciseType: exerciseForm.exerciseType,
      hanziAnswer: exerciseForm.hanziAnswer.trim(),
      pinyinPrompt: blankToNull(exerciseForm.pinyinPrompt),
      translationEn: blankToNull(exerciseForm.translationEn),
      translationRu: blankToNull(exerciseForm.translationRu),
      audioZhAssetId: exerciseForm.audioZhAssetId,
      explanation: blankToNull(exerciseForm.explanation),
      sortOrder: exerciseForm.sortOrder,
      status: exerciseForm.status,
      wordOptions: parseWordOptions(exerciseForm.wordOptionsText)
    }
    if (editingExercise.value) {
      await updateAdminSentenceExercise(editingExercise.value.id, payload)
    } else {
      await createAdminSentenceExercise(payload)
    }
    exerciseDialogVisible.value = false
    ElMessage.success(t('content.saved'))
    await loadSentenceExercises()
  } finally {
    submitting.value = false
  }
}

async function submitMaterial() {
  await materialFormRef.value?.validate()
  submitting.value = true
  try {
    const payload = {
      title: materialForm.title.trim(),
      parentId: materialForm.parentId,
      materialType: materialForm.materialType,
      description: blankToNull(materialForm.description),
      coverAssetId: materialForm.coverAssetId,
      status: materialForm.status
    }
    if (editingMaterial.value) {
      await updateAdminVideoMaterial(editingMaterial.value.id, payload)
    } else {
      await createAdminVideoMaterial(payload)
    }
    materialDialogVisible.value = false
    ElMessage.success(t('content.saved'))
    await loadMaterialOptions()
    await loadVideoMaterials()
  } finally {
    submitting.value = false
  }
}

async function submitLine() {
  await lineFormRef.value?.validate()
  if (!lineForm.materialId) {
    return
  }
  if (lineFormReadonly.value) {
    ElMessage.warning(t('content.parentReadonly.submitDisabled'))
    return
  }
  submitting.value = true
  try {
    const payload = {
      materialId: lineForm.materialId,
      lineNo: lineForm.lineNo,
      hanziText: lineForm.hanziText.trim(),
      pinyinText: blankToNull(lineForm.pinyinText),
      translationEn: blankToNull(lineForm.translationEn),
      translationRu: blankToNull(lineForm.translationRu),
      audioAssetId: lineForm.audioAssetId,
      startMs: lineForm.startMs,
      endMs: lineForm.endMs
    }
    if (editingLine.value) {
      await updateAdminDialogueLine(editingLine.value.id, payload)
    } else {
      await createAdminDialogueLine(payload)
    }
    lineDialogVisible.value = false
    ElMessage.success(t('content.saved'))
    await loadDialogueLines()
  } finally {
    submitting.value = false
  }
}

async function submitLineVocab() {
  await lineVocabFormRef.value?.validate()
  if (!lineVocabForm.dialogueLineId) {
    return
  }
  if (lineVocabFormReadonly.value) {
    ElMessage.warning(t('content.parentReadonly.submitDisabled'))
    return
  }
  submitting.value = true
  try {
    const payload = {
      dialogueLineId: lineVocabForm.dialogueLineId,
      vocabItemId: lineVocabForm.vocabItemId,
      wordText: lineVocabForm.wordText.trim(),
      pinyin: blankToNull(lineVocabForm.pinyin),
      meaningEn: blankToNull(lineVocabForm.meaningEn),
      meaningRu: blankToNull(lineVocabForm.meaningRu),
      explanation: blankToNull(lineVocabForm.explanation)
    }
    if (editingLineVocab.value) {
      await updateAdminDialogueLineVocab(editingLineVocab.value.id, payload)
    } else {
      await createAdminDialogueLineVocab(payload)
    }
    lineVocabDialogVisible.value = false
    ElMessage.success(t('content.saved'))
    await loadLineVocab()
  } finally {
    submitting.value = false
  }
}

async function deleteLineVocab(record: AdminDialogueLineVocab) {
  await ElMessageBox.confirm(t('content.validation.deleteConfirm'), t('content.statusDialogTitle'), {
    confirmButtonText: t('content.actions.delete'),
    cancelButtonText: t('content.cancel'),
    type: 'warning'
  })
  await deleteAdminDialogueLineVocab(record.id)
  ElMessage.success(t('content.saved'))
  await loadLineVocab()
}

async function submitUpload() {
  await uploadFormRef.value?.validate()
  const file = uploadForm.fileList[0]?.raw
  if (!file) {
    ElMessage.warning(t('content.validation.fileRequired'))
    return
  }
  const formData = new FormData()
  formData.append('file', file)
  formData.append('mediaType', uploadForm.mediaType)
  if (uploadForm.language) {
    formData.append('language', uploadForm.language)
  }
  if (uploadForm.durationMs !== null && uploadForm.durationMs !== undefined) {
    formData.append('durationMs', String(uploadForm.durationMs))
  }
  uploading.value = true
  try {
    const asset = await uploadAdminMediaAsset(formData)
    uploadDialogVisible.value = false
    ElMessage.success(t('content.saved'))
    if (asset.mediaType === 'audio') {
      if (uploadTarget.value === 'itemAudio') {
        itemForm.audioAssetId = asset.id
      }
      if (uploadTarget.value === 'exerciseAudio') {
        exerciseForm.audioZhAssetId = asset.id
      }
      if (uploadTarget.value === 'lineAudio') {
        lineForm.audioAssetId = asset.id
      }
      await loadAudioOptions()
    }
    if (asset.mediaType === 'image') {
      if (uploadTarget.value === 'materialCover') {
        materialForm.coverAssetId = asset.id
      }
      await loadImageOptions()
    }
    await loadMediaAssets()
  } finally {
    uploading.value = false
  }
}

async function submitBulkBind() {
  if (bulkBindTargetReadonly(bulkBindForm.target)) {
    ElMessage.warning(t('content.parentReadonly.submitDisabled'))
    return
  }
  const parsed = parseBulkBindingRows(bulkBindForm.rowsText)
  if (parsed.errors.length > 0) {
    await ElMessageBox.alert(parsed.errors.join('\n'), t('content.bulkBind.invalidTitle'), {
      confirmButtonText: t('content.submit'),
      type: 'warning'
    })
    return
  }
  if (parsed.bindings.length === 0) {
    ElMessage.warning(t('content.bulkBind.emptyRows'))
    return
  }
  bulkBindSubmitting.value = true
  try {
    const result = await submitBulkBindRequest({ bindings: parsed.bindings })
    bulkBindDialogVisible.value = false
    await reloadBulkBindTarget()
    const summary = t('content.bulkBind.result', {
      success: result.successCount,
      requested: result.requestedCount
    })
    if (result.errors.length > 0) {
      await ElMessageBox.alert([summary, ...result.errors].join('\n'), t('content.bulkBind.resultTitle'), {
        confirmButtonText: t('content.submit'),
        type: result.successCount > 0 ? 'warning' : 'error'
      })
      return
    }
    ElMessage.success(summary)
  } finally {
    bulkBindSubmitting.value = false
  }
}

function requiresImportContext(importType: AdminContentImportType) {
  return contextImportTypes.has(importType)
}

function supportsMultipleImportContexts(importType: AdminContentImportType) {
  return importType === 'vocab-items' || importType === 'sentence-exercises'
}

function selectedImportContextIds(importType: AdminContentImportType) {
  if (supportsMultipleImportContexts(importType)) {
    return [...csvImportForm.contextIds]
  }
  return csvImportForm.contextId ? [csvImportForm.contextId] : []
}

function defaultImportContextId(importType: AdminContentImportType) {
  if (importType === 'vocab-items') {
    return itemQuery.vocabListId || appliedItemVocabListId.value || null
  }
  if (importType === 'sentence-exercises') {
    return null
  }
  if (importType === 'dialogue-lines') {
    return lineQuery.materialId || appliedLineMaterialId.value || null
  }
  if (importType === 'dialogue-line-vocab') {
    return lineVocabQuery.dialogueLineId || appliedLineVocabDialogueLineId.value || null
  }
  return null
}

function defaultImportContextIds(importType: AdminContentImportType) {
  const contextId = defaultImportContextId(importType)
  return contextId ? [contextId] : []
}

function importContextTarget(importType: AdminContentImportType) {
  if (importType === 'vocab-items') {
    return t('content.importContext.targets.vocabList')
  }
  if (importType === 'sentence-exercises') {
    return t('content.importContext.targets.exerciseSet')
  }
  if (importType === 'dialogue-lines') {
    return t('content.importContext.targets.material')
  }
  if (importType === 'dialogue-line-vocab') {
    return t('content.importContext.targets.dialogueLine')
  }
  return ''
}

function importContextName(importType: AdminContentImportType, contextId: number | null) {
  if (!contextId) {
    return ''
  }
  if (importType === 'vocab-items') {
    const list = listOptions.value.find(item => item.id === contextId)
    return list ? listOptionLabel(list) : ''
  }
  if (importType === 'sentence-exercises') {
    const set = setOptions.value.find(item => item.id === contextId)
    return set ? exerciseSetOptionLabel(set) : ''
  }
  if (importType === 'dialogue-lines') {
    const material = materialOptions.value.find(item => item.id === contextId)
    return material ? materialOptionLabel(material) : ''
  }
  if (importType === 'dialogue-line-vocab') {
    const line = findDialogueLine(contextId)
    return line ? lineOptionLabel(line) : ''
  }
  return ''
}

function importContextNames(importType: AdminContentImportType, contextIds: number[]) {
  return contextIds
    .map(id => importContextName(importType, id))
    .filter(Boolean)
    .join('、')
}

async function prepareImportContextOptions(importType: AdminContentImportType) {
  if (importType === 'vocab-items') {
    await loadListOptions()
    return
  }
  if (importType === 'sentence-exercises') {
    await loadExerciseSetOptions()
    return
  }
  if (importType === 'dialogue-lines') {
    await loadMaterialOptions()
    return
  }
  if (importType === 'dialogue-line-vocab') {
    await loadLineOptions(appliedLineVocabMaterialId.value || lineVocabQuery.materialId)
  }
}

async function downloadTemplate(importType: AdminContentImportType) {
  templateDownloading.value = true
  try {
    const contextIds = selectedImportContextIds(importType)
    const contextId = contextIds[0] || null
    if (requiresImportContext(importType) && contextIds.length === 0) {
      ElMessage.warning(t('content.importContext.required', { target: importContextTarget(importType) }))
      return
    }
    await prepareImportContextOptions(importType)
    if (contextIds.some(id => isImportContextInactive(importType, id))) {
      ElMessage.warning(t('content.parentReadonly.importDisabled'))
      return
    }
    const template = await downloadAdminContentImportTemplate(importType, {
      contextId,
      contextIds,
      contextName: importContextNames(importType, contextIds) || importContextName(importType, contextId)
    })
    const blob = new Blob([template.content], { type: 'text/csv;charset=utf-8' })
    saveBlob(blob, template.filename || `${importType}-template.csv`)
    ElMessage.success(t('content.templateDownloaded'))
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('content.templateDownloadFailed'))
  } finally {
    templateDownloading.value = false
  }
}

async function submitCsvImport() {
  const file = csvImportForm.fileList[0]?.raw
  clearCsvImportError()
  if (!file) {
    ElMessage.warning(t('content.validation.fileRequired'))
    return
  }
  const contextIds = selectedImportContextIds(csvImportForm.importType)
  if (requiresImportContext(csvImportForm.importType) && contextIds.length === 0) {
    ElMessage.warning(t('content.importContext.required', { target: importContextTarget(csvImportForm.importType) }))
    return
  }
  if (csvImportContextReadonly.value) {
    ElMessage.warning(t('content.parentReadonly.importDisabled'))
    return
  }
  const formData = new FormData()
  formData.append('file', file)
  csvImportSubmitting.value = true
  try {
    const importType = csvImportForm.importType
    const result = await importAdminContentCsv(importType, formData, contextIds[0] || null, contextIds)
    await reloadImportType(importType)
    const summary = t('content.importResult', {
      success: result.successCount,
      requested: result.requestedCount
    })
    if (result.errors.length > 0) {
      csvImportLastError.value = summary
      csvImportLastErrorHint.value = t('content.importErrorFixHint')
      await ElMessageBox.alert(
        [summary, t('content.importErrorFixHint'), '', ...result.errors].join('\n'),
        t('content.importResultTitle'),
        {
          confirmButtonText: t('content.submit'),
          type: result.successCount > 0 ? 'warning' : 'error'
        }
      ).catch(() => undefined)
      if (result.successCount > 0) {
        closeCsvImportDialog()
      }
      return
    }
    closeCsvImportDialog()
    ElMessage.success(summary)
  } catch (error) {
    csvImportLastError.value = error instanceof Error ? error.message : t('content.importFailed')
    csvImportLastErrorHint.value = t('content.importRequestErrorHint')
    await ElMessageBox.alert(importFailureMessage(error), t('content.importFailed'), {
      confirmButtonText: t('content.submit'),
      type: 'error'
    }).catch(() => undefined)
  } finally {
    csvImportSubmitting.value = false
  }
}

function closeCsvImportDialog() {
  csvImportDialogVisible.value = false
  csvImportForm.contextId = null
  csvImportForm.contextIds = []
  csvImportForm.fileList = []
  clearCsvImportError()
}

function clearCsvImportError() {
  csvImportLastError.value = ''
  csvImportLastErrorHint.value = ''
}

function importFailureMessage(error: unknown) {
  const lines = [
    error instanceof Error ? error.message : t('content.importFailed'),
    '',
    t('content.importRequestErrorHint')
  ]
  if (error instanceof ApiError && error.traceId) {
    lines.push('', t('content.importTraceId', { traceId: error.traceId }))
  }
  return lines.join('\n')
}

function setSelectedLists(rows: AdminVocabList[]) {
  selectedLists.value = rows
}

function setSelectedItems(rows: AdminVocabItem[]) {
  selectedItems.value = rows
}

function setSelectedMediaAssets(rows: AdminMediaAsset[]) {
  selectedMediaAssets.value = rows
}

function setSelectedSets(rows: AdminExerciseSet[]) {
  selectedSets.value = rows
}

function setSelectedExercises(rows: AdminSentenceExercise[]) {
  selectedExercises.value = rows
}

function setSelectedMaterials(rows: AdminVideoMaterial[]) {
  selectedMaterials.value = rows
}

function selectedRowsForBatchStatus(target: BatchStatusTarget): Array<{ id: number }> {
  if (target === 'lists') {
    return selectedLists.value
  }
  if (target === 'items') {
    return selectedItems.value
  }
  if (target === 'media') {
    return selectedMediaAssets.value
  }
  if (target === 'sets') {
    return selectedSets.value
  }
  if (target === 'exercises') {
    return selectedExercises.value
  }
  return selectedMaterials.value
}

function clearBatchStatusSelection(target: BatchStatusTarget) {
  if (target === 'lists') {
    selectedLists.value = []
  } else if (target === 'items') {
    selectedItems.value = []
  } else if (target === 'media') {
    selectedMediaAssets.value = []
  } else if (target === 'sets') {
    selectedSets.value = []
  } else if (target === 'exercises') {
    selectedExercises.value = []
  } else {
    selectedMaterials.value = []
  }
}

function selectedRowsForBatchAssignment(target: BatchAssignmentTarget): Array<{ id: number }> {
  return target === 'items' ? selectedItems.value : selectedExercises.value
}

function defaultBatchAssignmentTargetIds(target: BatchAssignmentTarget) {
  if (target === 'items' && itemQuery.vocabListId) {
    return [itemQuery.vocabListId]
  }
  if (target === 'exercises' && exerciseQuery.exerciseSetId) {
    return [exerciseQuery.exerciseSetId]
  }
  return []
}

function openBatchAssignmentDialog(target: BatchAssignmentTarget) {
  const rows = selectedRowsForBatchAssignment(target)
  if (!rows.length) {
    ElMessage.warning(t('content.batchStatus.emptySelection'))
    return
  }
  if ((target === 'items' && selectedItemsReadonly.value) || (target === 'exercises' && selectedExercisesReadonly.value)) {
    ElMessage.warning(t('content.parentReadonly.submitDisabled'))
    return
  }
  batchAssignmentTarget.value = target
  batchAssignmentForm.targetIds = defaultBatchAssignmentTargetIds(target)
  batchAssignmentDialogVisible.value = true
}

async function reloadBatchAssignmentTarget(target: BatchAssignmentTarget) {
  if (target === 'items') {
    await loadItems()
    await loadVocabItemOptions()
    selectedItems.value = []
    return
  }
  await loadSentenceExercises()
  selectedExercises.value = []
}

async function submitBatchAssignment() {
  const target = batchAssignmentTarget.value
  const rows = selectedRowsForBatchAssignment(target)
  if (!rows.length) {
    ElMessage.warning(t('content.batchStatus.emptySelection'))
    return
  }
  const targetIds = [...new Set(batchAssignmentForm.targetIds)].filter(Boolean)
  if (!targetIds.length) {
    ElMessage.warning(t('content.batchAssignments.emptyTarget'))
    return
  }
  batchAssignmentSubmitting.value = true
  try {
    const payload = {
      ids: rows.map(row => row.id),
      targetIds
    }
    const result =
      target === 'items'
        ? await updateAdminVocabItemListAssignments(payload)
        : await updateAdminSentenceExerciseSetAssignments(payload)
    await reloadBatchAssignmentTarget(target)
    batchAssignmentDialogVisible.value = false
    const summary = t('content.batchAssignments.result', {
      success: result.successCount,
      requested: result.requestedCount
    })
    if (result.errors.length) {
      await ElMessageBox.alert([summary, ...result.errors].join('\n'), t('content.batchAssignments.resultTitle'), {
        confirmButtonText: t('content.submit'),
        type: result.successCount > 0 ? 'warning' : 'error'
      })
      return
    }
    ElMessage.success(summary)
  } finally {
    batchAssignmentSubmitting.value = false
  }
}

async function removeVocabItemListAssignment(item: AdminVocabItem, listId: number | null) {
  if (listId === null) {
    return
  }
  const targetIds = assignedVocabListIds(item).filter(id => id !== listId)
  try {
    await updateAdminVocabItem(item.id, vocabItemAssignmentPayload(item, targetIds))
    await loadItems()
    await loadVocabItemOptions()
    ElMessage.success(t('content.batchAssignments.removeSuccess'))
  } catch (error) {
    showAssignmentRemoveError(error)
  }
}

async function removeSentenceExerciseSetAssignment(exercise: AdminSentenceExercise, setId: number | null) {
  if (setId === null) {
    return
  }
  const targetIds = assignedExerciseSetIds(exercise).filter(id => id !== setId)
  try {
    await updateAdminSentenceExercise(exercise.id, sentenceExerciseAssignmentPayload(exercise, targetIds))
    await loadSentenceExercises()
    ElMessage.success(t('content.batchAssignments.removeSuccess'))
  } catch (error) {
    showAssignmentRemoveError(error)
  }
}

function vocabItemAssignmentPayload(item: AdminVocabItem, targetIds: number[]) {
  return {
    vocabListId: targetIds[0] || null,
    vocabListIds: targetIds,
    hanzi: item.hanzi,
    pinyin: item.pinyin,
    meaningEn: item.meaningEn,
    meaningRu: item.meaningRu,
    exampleSentence: item.exampleSentence,
    audioAssetId: item.audioAssetId,
    sortOrder: item.sortOrder,
    status: item.status
  }
}

function sentenceExerciseAssignmentPayload(exercise: AdminSentenceExercise, targetIds: number[]) {
  return {
    exerciseSetId: targetIds[0] || null,
    exerciseSetIds: targetIds,
    exerciseType: exercise.exerciseType,
    hanziAnswer: exercise.hanziAnswer,
    pinyinPrompt: exercise.pinyinPrompt,
    translationEn: exercise.translationEn,
    translationRu: exercise.translationRu,
    audioZhAssetId: exercise.audioZhAssetId,
    explanation: exercise.explanation,
    sortOrder: exercise.sortOrder,
    status: exercise.status,
    wordOptions: exercise.wordOptions.map(option => ({
      wordText: option.wordText,
      correctOrder: option.correctOrder
    }))
  }
}

function showAssignmentRemoveError(error: unknown) {
  if (error instanceof ApiError) {
    ElMessage.error(error.message)
    return
  }
  ElMessage.error(t('content.batchAssignments.removeFailed'))
}

async function updateBatchStatus(
  target: BatchStatusTarget,
  ids: number[],
  status: ContentStatus,
  reason: string
): Promise<AdminBatchContentStatusResult> {
  if (target === 'lists') {
    return updateAdminVocabListStatuses({ ids, status, reason })
  }
  if (target === 'items') {
    return updateAdminVocabItemStatuses({ ids, status, reason })
  }
  if (target === 'media') {
    return updateAdminMediaAssetStatuses({ ids, status, reason })
  }
  if (target === 'sets') {
    return updateAdminExerciseSetStatuses({ ids, status, reason })
  }
  if (target === 'exercises') {
    return updateAdminSentenceExerciseStatuses({ ids, status, reason })
  }
  return updateAdminVideoMaterialStatuses({ ids, status, reason })
}

async function reloadBatchStatusTarget(target: BatchStatusTarget) {
  if (target === 'lists') {
    await loadListOptions()
    await loadLists()
  } else if (target === 'items') {
    await loadItems()
    await loadVocabItemOptions()
  } else if (target === 'media') {
    await loadMediaAssets()
    await loadAudioOptions()
    await loadImageOptions()
  } else if (target === 'sets') {
    await loadExerciseSetOptions()
    await loadExerciseSets()
  } else if (target === 'exercises') {
    await loadSentenceExercises()
  } else {
    await loadMaterialOptions()
    await loadVideoMaterials()
  }
  clearBatchStatusSelection(target)
}

async function submitBatchStatus(target: BatchStatusTarget, status: ContentStatus) {
  const rows = selectedRowsForBatchStatus(target)
  if (!rows.length) {
    ElMessage.warning(t('content.batchStatus.emptySelection'))
    return
  }
  if ((target === 'items' && selectedItemsReadonly.value) || (target === 'exercises' && selectedExercisesReadonly.value)) {
    ElMessage.warning(t('content.parentReadonly.submitDisabled'))
    return
  }
  const action = status === 'active' ? t('content.actions.batchEnable') : t('content.actions.batchDisable')
  const targetLabel = t(`content.batchStatus.targets.${target}`)
  const { value } = await ElMessageBox.prompt(
    t('content.batchStatus.reasonPlaceholder'),
    t('content.batchStatus.confirmTitle', { action, target: targetLabel, count: rows.length }),
    {
      confirmButtonText: t('content.submit'),
      cancelButtonText: t('content.cancel'),
      inputType: 'textarea',
      inputValidator: inputValue => !inputValue || inputValue.length <= 1000 || t('content.reasonTooLong')
    }
  )
  batchStatusSubmitting.value = true
  try {
    const result = await updateBatchStatus(
      target,
      rows.map(row => row.id),
      status,
      value || ''
    )
    await reloadBatchStatusTarget(target)
    const summary = t('content.batchStatus.result', {
      action,
      success: result.successCount,
      requested: result.requestedCount
    })
    if (result.errors.length) {
      await ElMessageBox.alert([summary, ...result.errors].join('\n'), t('content.batchStatus.resultTitle'), {
        confirmButtonText: t('content.submit'),
        type: result.successCount > 0 ? 'warning' : 'error'
      })
      return
    }
    ElMessage.success(summary)
  } finally {
    batchStatusSubmitting.value = false
  }
}

async function toggleListStatus(list: AdminVocabList) {
  const status: ContentStatus = list.status === 'active' ? 'inactive' : 'active'
  const { value } = await ElMessageBox.prompt(t('content.statusReasonPlaceholder'), t('content.statusDialogTitle'), {
    confirmButtonText: t('content.submit'),
    cancelButtonText: t('content.cancel'),
    inputType: 'textarea',
    inputValidator: value => !value || value.length <= 1000 || t('content.reasonTooLong')
  })
  await updateAdminVocabListStatus(list.id, { status, reason: value || '' })
  ElMessage.success(t('content.saved'))
  await loadListOptions()
  await loadLists()
}

async function toggleItemStatus(item: AdminVocabItem) {
  const status: ContentStatus = item.status === 'active' ? 'inactive' : 'active'
  const { value } = await ElMessageBox.prompt(t('content.statusReasonPlaceholder'), t('content.statusDialogTitle'), {
    confirmButtonText: t('content.submit'),
    cancelButtonText: t('content.cancel'),
    inputType: 'textarea',
    inputValidator: value => !value || value.length <= 1000 || t('content.reasonTooLong')
  })
  await updateAdminVocabItemStatus(item.id, { status, reason: value || '' })
  ElMessage.success(t('content.saved'))
  await loadItems()
}

async function toggleMediaStatus(asset: AdminMediaAsset) {
  const status: ContentStatus = asset.status === 'active' ? 'inactive' : 'active'
  const { value } = await ElMessageBox.prompt(t('content.statusReasonPlaceholder'), t('content.statusDialogTitle'), {
    confirmButtonText: t('content.submit'),
    cancelButtonText: t('content.cancel'),
    inputType: 'textarea',
    inputValidator: value => !value || value.length <= 1000 || t('content.reasonTooLong')
  })
  await updateAdminMediaAssetStatus(asset.id, { status, reason: value || '' })
  ElMessage.success(t('content.saved'))
  await loadMediaAssets()
  await loadAudioOptions()
  await loadImageOptions()
}

async function deleteMediaAsset(asset: AdminMediaAsset) {
  await ElMessageBox.confirm(t('content.validation.mediaDeleteConfirm'), t('content.statusDialogTitle'), {
    confirmButtonText: t('content.actions.delete'),
    cancelButtonText: t('content.cancel'),
    type: 'warning'
  })
  await deleteAdminMediaAsset(asset.id)
  ElMessage.success(t('content.saved'))
  await loadMediaAssets()
  await loadAudioOptions()
  await loadImageOptions()
}

async function toggleSetStatus(set: AdminExerciseSet) {
  const status: ContentStatus = set.status === 'active' ? 'inactive' : 'active'
  const { value } = await ElMessageBox.prompt(t('content.statusReasonPlaceholder'), t('content.statusDialogTitle'), {
    confirmButtonText: t('content.submit'),
    cancelButtonText: t('content.cancel'),
    inputType: 'textarea',
    inputValidator: value => !value || value.length <= 1000 || t('content.reasonTooLong')
  })
  await updateAdminExerciseSetStatus(set.id, { status, reason: value || '' })
  ElMessage.success(t('content.saved'))
  await loadExerciseSetOptions()
  await loadExerciseSets()
}

async function toggleExerciseStatus(exercise: AdminSentenceExercise) {
  const status: ContentStatus = exercise.status === 'active' ? 'inactive' : 'active'
  const { value } = await ElMessageBox.prompt(t('content.statusReasonPlaceholder'), t('content.statusDialogTitle'), {
    confirmButtonText: t('content.submit'),
    cancelButtonText: t('content.cancel'),
    inputType: 'textarea',
    inputValidator: value => !value || value.length <= 1000 || t('content.reasonTooLong')
  })
  await updateAdminSentenceExerciseStatus(exercise.id, { status, reason: value || '' })
  ElMessage.success(t('content.saved'))
  await loadSentenceExercises()
}

async function toggleMaterialStatus(material: AdminVideoMaterial) {
  const status: ContentStatus = material.status === 'active' ? 'inactive' : 'active'
  const { value } = await ElMessageBox.prompt(t('content.statusReasonPlaceholder'), t('content.statusDialogTitle'), {
    confirmButtonText: t('content.submit'),
    cancelButtonText: t('content.cancel'),
    inputType: 'textarea',
    inputValidator: value => !value || value.length <= 1000 || t('content.reasonTooLong')
  })
  await updateAdminVideoMaterialStatus(material.id, { status, reason: value || '' })
  ElMessage.success(t('content.saved'))
  await loadMaterialOptions()
  await loadVideoMaterials()
}

function listOptionLabel(list: AdminVocabList) {
  return `${list.parentName ? `${list.parentName} / ` : ''}${list.name}${list.level ? ` / ${list.level}` : ''}${
    list.status === 'inactive' ? ` / ${t('content.status.inactive')}` : ''
  }`
}

function exerciseSetOptionLabel(set: AdminExerciseSet) {
  return `${set.parentTitle ? `${set.parentTitle} / ` : ''}${set.title}${set.level ? ` / ${set.level}` : ''} / ${t(`content.exerciseTypes.${set.exerciseType}`)}${
    set.status === 'inactive' ? ` / ${t('content.status.inactive')}` : ''
  }`
}

function materialOptionLabel(material: AdminVideoMaterial) {
  const recordById = new Map(materialOptions.value.map(record => [record.id, record]))
  return videoMaterialTreeSelectFullLabel(material, recordById)
}

function lineOptionLabel(line: AdminDialogueLine) {
  return `#${line.lineNo} ${line.hanziText}${line.materialStatus === 'inactive' ? ` / ${t('content.status.inactive')}` : ''}`
}

function vocabItemOptionLabel(item: AdminVocabItem) {
  return `${item.hanzi}${item.pinyin ? ` / ${item.pinyin}` : ''}`
}

function assignedVocabListIds(item: AdminVocabItem) {
  return [...new Set(item.vocabListIds?.length ? item.vocabListIds : item.vocabListId ? [item.vocabListId] : [])]
}

function assignedExerciseSetIds(exercise: AdminSentenceExercise) {
  return [...new Set(exercise.exerciseSetIds?.length ? exercise.exerciseSetIds : exercise.exerciseSetId ? [exercise.exerciseSetId] : [])]
}

function vocabListPathLabel(listId: number, fallback?: string) {
  const recordById = vocabListRecordById.value
  const list = recordById.get(listId)
  if (!list) {
    return fallback || `#${listId}`
  }
  return buildVocabListNamePath(list, recordById).join(' / ')
}

function vocabItemListTagItems(item: AdminVocabItem): AssignmentTagItem[] {
  const listIds = assignedVocabListIds(item)
  if (listIds.length) {
    return listIds.map((listId, index) => ({
      id: listId,
      label: vocabListPathLabel(listId, item.vocabListNames?.[index] || item.vocabListName || `#${listId}`)
    }))
  }
  if (item.vocabListNames?.length) {
    return item.vocabListNames.map(label => ({ id: null, label }))
  }
  if (item.vocabListName) {
    return [{ id: null, label: item.vocabListName }]
  }
  return []
}

function exerciseSetPathLabel(setId: number, fallback?: string) {
  const recordById = exerciseSetRecordById.value
  const set = recordById.get(setId)
  if (!set) {
    return fallback || `#${setId}`
  }
  return buildExerciseSetTitlePath(set, recordById).join(' / ')
}

function sentenceExerciseSetTagItems(exercise: AdminSentenceExercise): AssignmentTagItem[] {
  const setIds = assignedExerciseSetIds(exercise)
  if (setIds.length) {
    return setIds.map((setId, index) => ({
      id: setId,
      label: exerciseSetPathLabel(setId, exercise.exerciseSetTitles?.[index] || exercise.exerciseSetTitle || `#${setId}`)
    }))
  }
  if (exercise.exerciseSetTitles?.length) {
    return exercise.exerciseSetTitles.map(label => ({ id: null, label }))
  }
  if (exercise.exerciseSetTitle) {
    return [{ id: null, label: exercise.exerciseSetTitle }]
  }
  return []
}

function mediaOptionLabel(asset: AdminMediaAsset) {
  return `#${asset.id} ${asset.language ? t(`content.languages.${asset.language}`) : ''} ${mediaPrimaryLabel(asset)}`
}

function mediaPrimaryLabel(asset: AdminMediaAsset) {
  return asset.originalFilename || filenameFromUrl(asset.url) || asset.url
}

function filenameFromUrl(url: string) {
  if (!url) {
    return ''
  }
  const cleanUrl = url.split('?')[0] || ''
  const index = cleanUrl.lastIndexOf('/')
  const filename = index >= 0 ? cleanUrl.substring(index + 1) : cleanUrl
  try {
    return decodeURIComponent(filename)
  } catch {
    return filename
  }
}

function wordOptionsToText(exercise?: AdminSentenceExercise) {
  if (!exercise?.wordOptions?.length) {
    return ''
  }
  return [...exercise.wordOptions]
    .sort((left, right) => left.correctOrder - right.correctOrder)
    .map(option => option.wordText)
    .join('\n')
}

function parseWordOptions(value: string) {
  return value
    .split('\n')
    .map(item => item.trim())
    .filter(Boolean)
    .map((wordText, index) => ({
      wordText,
      correctOrder: index + 1
    }))
}

function parseBulkBindingRows(value: string) {
  const bindings: AdminBatchBindMediaAssetPayload['bindings'] = []
  const errors: string[] = []
  value.split(/\r?\n/).forEach((rawLine, index) => {
    const line = rawLine.trim()
    if (!line) {
      return
    }
    const columns = line.split(/[,\s，\t]+/).filter(Boolean)
    if (columns.length < 2) {
      errors.push(t('content.bulkBind.lineFormatError', { line: index + 1 }))
      return
    }
    const targetId = Number(columns[0])
    const mediaAssetId = Number(columns[1])
    if (!Number.isInteger(targetId) || targetId <= 0 || !Number.isInteger(mediaAssetId) || mediaAssetId <= 0) {
      errors.push(t('content.bulkBind.lineFormatError', { line: index + 1 }))
      return
    }
    bindings.push({ targetId, mediaAssetId })
  })
  return { bindings, errors }
}

function submitBulkBindRequest(payload: AdminBatchBindMediaAssetPayload) {
  if (bulkBindForm.target === 'itemAudio') {
    return bindAdminVocabItemAudio(payload)
  }
  if (bulkBindForm.target === 'exerciseAudio') {
    return bindAdminSentenceExerciseAudio(payload)
  }
  if (bulkBindForm.target === 'lineAudio') {
    return bindAdminDialogueLineAudio(payload)
  }
  return bindAdminVideoMaterialCover(payload)
}

async function reloadBulkBindTarget() {
  if (bulkBindForm.target === 'itemAudio') {
    await loadItems()
    return
  }
  if (bulkBindForm.target === 'exerciseAudio') {
    await loadSentenceExercises()
    return
  }
  if (bulkBindForm.target === 'lineAudio') {
    await loadDialogueLines()
    return
  }
  await loadMaterialOptions()
  await loadVideoMaterials()
}

async function reloadImportType(importType: AdminContentImportType) {
  if (importType === 'vocab-lists') {
    await loadListOptions()
    await loadLists()
    return
  }
  if (importType === 'vocab-items') {
    await loadItems()
    await loadVocabItemOptions()
    return
  }
  if (importType === 'exercise-sets') {
    await loadExerciseSetOptions()
    await loadExerciseSets()
    return
  }
  if (importType === 'sentence-exercises') {
    await loadSentenceExercises()
    return
  }
  if (importType === 'video-materials') {
    await loadMaterialOptions()
    await loadVideoMaterials()
    return
  }
  if (importType === 'dialogue-lines') {
    await loadDialogueLines()
    await loadLineOptions(lineVocabQuery.materialId)
    return
  }
  await loadLineVocab()
}

function handleUploadExceed() {
  ElMessage.warning(t('content.validation.fileLimit'))
}

function handleCsvImportExceed() {
  ElMessage.warning(t('content.validation.fileLimit'))
}

function saveBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = filename
  anchor.style.display = 'none'
  document.body.appendChild(anchor)
  anchor.click()
  document.body.removeChild(anchor)
  window.setTimeout(() => URL.revokeObjectURL(url), 1000)
}

function blankToNull(value: string) {
  const next = value.trim()
  return next ? next : null
}

function formatDate(value?: string | null) {
  if (!value) {
    return t('common.empty')
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return t('common.empty')
  }
  return new Intl.DateTimeFormat(locale.value, {
    dateStyle: 'medium',
    timeStyle: 'short'
  }).format(date)
}

function formatFileSize(value?: number | null) {
  if (!value) {
    return t('common.empty')
  }
  if (value < 1024) {
    return `${value} B`
  }
  if (value < 1024 * 1024) {
    return `${(value / 1024).toFixed(1)} KB`
  }
  return `${(value / 1024 / 1024).toFixed(1)} MB`
}

function formatDurationMs(value?: number | null) {
  if (value === null || value === undefined) {
    return t('common.empty')
  }
  const seconds = Math.round(value / 1000)
  const minutes = Math.floor(seconds / 60)
  const remainingSeconds = seconds % 60
  return `${minutes}:${String(remainingSeconds).padStart(2, '0')}`
}

function formatTimeRange(startMs?: number | null, endMs?: number | null) {
  if (startMs === null && endMs === null) {
    return t('common.empty')
  }
  const start = startMs === null || startMs === undefined ? t('common.empty') : formatDurationMs(startMs)
  const end = endMs === null || endMs === undefined ? t('common.empty') : formatDurationMs(endMs)
  return `${start} - ${end}`
}

function routeText(key: string) {
  const value = route.query[key]
  if (Array.isArray(value)) {
    return value[0] || ''
  }
  return value || ''
}

function routeNumber(key: string, fallback: number) {
  const value = Number(routeText(key))
  return Number.isInteger(value) && value > 0 ? value : fallback
}

function routeBoolean(key: string): boolean | null {
  const value = routeText(key)
  if (value === 'true') {
    return true
  }
  if (value === 'false') {
    return false
  }
  return null
}

function isContentTab(value: string): value is ContentTab {
  return value === 'lists' || value === 'items' || value === 'media' || value === 'sets' || value === 'exercises' || value === 'materials' || value === 'lines' || value === 'lineVocab'
}

function isContentStatus(value: string): value is ContentStatus {
  return value === 'active' || value === 'inactive'
}

function routeContentStatus(value: string): ContentStatus | '' {
  if (value === 'all') {
    return ''
  }
  return isContentStatus(value) ? value : defaultContentStatus
}

function isVocabListType(value: string): value is VocabListType {
  return vocabTypes.includes(value as VocabListType)
}

function isExerciseType(value: string): value is ExerciseType {
  return exerciseTypes.includes(value as ExerciseType)
}

function isMediaType(value: string): value is MediaType {
  return value === 'audio' || value === 'image' || value === 'video'
}

function isMediaLanguage(value: string): value is MediaLanguage {
  return value === 'zh' || value === 'ru' || value === 'en'
}

function isVideoMaterialType(value: string): value is VideoMaterialType {
  return materialTypes.includes(value as VideoMaterialType)
}

function applyRouteQuery() {
  const tab = routeText('tab')
  const status = routeText('status')
  const page = routeNumber('page', 1)
  const pageSize = routeNumber('pageSize', 20)
  activeTab.value = isContentTab(tab) ? tab : 'lists'

  if (activeTab.value === 'lists') {
    const listType = routeText('listType')
    listQuery.page = page
    listQuery.pageSize = pageSize
    listQuery.keyword = routeText('keyword')
    listQuery.listType = isVocabListType(listType) ? listType : ''
    listQuery.level = routeText('level')
    listQuery.status = routeContentStatus(status)
  }

  if (activeTab.value === 'items') {
    itemQuery.page = page
    itemQuery.pageSize = pageSize
    itemQuery.vocabListId = routeNumber('vocabListId', 0) || null
    itemQuery.keyword = routeText('keyword')
    itemQuery.status = routeContentStatus(status)
    itemQuery.hasAudio = routeBoolean('hasAudio')
  }

  if (activeTab.value === 'media') {
    const mediaType = routeText('mediaType')
    const language = routeText('language')
    mediaQuery.page = page
    mediaQuery.pageSize = pageSize
    mediaQuery.keyword = routeText('keyword')
    mediaQuery.mediaType = isMediaType(mediaType) ? mediaType : ''
    mediaQuery.language = isMediaLanguage(language) ? language : ''
    mediaQuery.status = routeContentStatus(status)
  }

  if (activeTab.value === 'sets') {
    const exerciseType = routeText('exerciseType')
    setQuery.page = page
    setQuery.pageSize = pageSize
    setQuery.keyword = routeText('keyword')
    setQuery.exerciseType = isExerciseType(exerciseType) ? exerciseType : ''
    setQuery.level = routeText('level')
    setQuery.status = routeContentStatus(status)
  }

  if (activeTab.value === 'exercises') {
    const exerciseType = routeText('exerciseType')
    exerciseQuery.page = page
    exerciseQuery.pageSize = pageSize
    exerciseQuery.exerciseSetId = routeNumber('exerciseSetId', 0) || null
    exerciseQuery.keyword = routeText('keyword')
    exerciseQuery.exerciseType = isExerciseType(exerciseType) ? exerciseType : ''
    exerciseQuery.status = routeContentStatus(status)
    exerciseQuery.hasAudio = routeBoolean('hasAudio')
  }

  if (activeTab.value === 'materials') {
    const materialType = routeText('materialType')
    materialQuery.page = page
    materialQuery.pageSize = pageSize
    materialQuery.keyword = routeText('keyword')
    materialQuery.materialType = isVideoMaterialType(materialType) ? materialType : ''
    materialQuery.status = routeContentStatus(status)
    materialQuery.hasCover = routeBoolean('hasCover')
  }

  if (activeTab.value === 'lines') {
    lineQuery.page = page
    lineQuery.pageSize = pageSize
    lineQuery.materialId = routeNumber('materialId', 0) || null
    lineQuery.keyword = routeText('keyword')
    lineQuery.hasAudio = routeBoolean('hasAudio')
  }

  if (activeTab.value === 'lineVocab') {
    lineVocabQuery.page = page
    lineVocabQuery.pageSize = pageSize
    lineVocabQuery.materialId = routeNumber('materialId', 0) || null
    lineVocabQuery.dialogueLineId = routeNumber('dialogueLineId', 0) || null
    lineVocabQuery.keyword = routeText('keyword')
  }
}

onMounted(async () => {
  applyRouteQuery()
  await Promise.all([
    loadListOptions(),
    loadVocabItemOptions(),
    loadAudioOptions(),
    loadImageOptions(),
    loadExerciseSetOptions(),
    loadMaterialOptions()
  ])
  if (activeTab.value === 'lineVocab') {
    await loadLineOptions(lineVocabQuery.materialId)
  }
  reloadActive()
})
</script>

<style scoped>
.content-page {
  display: grid;
  gap: 16px;
}

.page-heading {
  align-items: center;
  display: flex;
  justify-content: space-between;
}

h1,
p {
  margin: 0;
}

h1 {
  color: #172033;
  font-size: 24px;
}

.page-heading p {
  color: #64748b;
  margin-top: 8px;
}

.admin-tabs {
  display: grid;
  gap: 14px;
}

.filter-card :deep(.el-card__body) {
  padding-bottom: 0;
}

.filter-card {
  margin-bottom: 16px;
}

.list-filter-form,
.item-filter-form,
.media-filter-form,
.set-filter-form,
.exercise-filter-form,
.material-filter-form,
.line-filter-form,
.line-vocab-filter-form {
  display: grid;
  gap: 12px;
}

.list-filter-form {
  grid-template-columns: minmax(240px, 320px) 150px 130px 150px auto;
}

.item-filter-form {
  align-items: flex-start;
  display: flex;
  flex-wrap: wrap;
}

.media-filter-form {
  grid-template-columns: minmax(260px, 360px) 150px 150px 150px auto;
}

.set-filter-form {
  grid-template-columns: minmax(240px, 320px) 180px 130px 150px auto;
}

.exercise-filter-form {
  align-items: flex-start;
  display: flex;
  flex-wrap: wrap;
}

.item-filter-form > .el-form-item,
.exercise-filter-form > .el-form-item {
  margin-bottom: 16px;
  min-width: 0;
}

.item-filter-form > .el-form-item:nth-child(1) {
  flex: 1 1 420px;
  max-width: 560px;
}

.item-filter-form > .el-form-item:nth-child(2) {
  flex: 1 1 460px;
  max-width: 640px;
}

.item-filter-form > .el-form-item:nth-child(3) {
  flex: 0 0 160px;
}

.item-filter-form > .el-form-item:nth-child(4) {
  flex: 0 0 180px;
}

.exercise-filter-form > .el-form-item:nth-child(1) {
  flex: 1 1 380px;
  max-width: 520px;
}

.exercise-filter-form > .el-form-item:nth-child(2) {
  flex: 1 1 420px;
  max-width: 600px;
}

.exercise-filter-form > .el-form-item:nth-child(3) {
  flex: 0 0 190px;
}

.exercise-filter-form > .el-form-item:nth-child(4) {
  flex: 0 0 160px;
}

.exercise-filter-form > .el-form-item:nth-child(5) {
  flex: 0 0 180px;
}

.item-filter-form > .filter-actions,
.exercise-filter-form > .filter-actions {
  flex: 0 0 auto;
  margin-left: auto;
  min-width: 220px;
}

.item-filter-form > .filter-actions :deep(.el-form-item__content),
.exercise-filter-form > .filter-actions :deep(.el-form-item__content) {
  flex-wrap: nowrap;
}

.material-filter-form {
  grid-template-columns: minmax(240px, 320px) 150px 130px 140px auto;
}

.line-filter-form {
  grid-template-columns: minmax(240px, 320px) 240px 140px auto;
}

.line-vocab-filter-form {
  grid-template-columns: minmax(240px, 320px) 240px 240px auto;
}

.filter-actions {
  justify-content: flex-end;
}

.filter-actions :deep(.el-form-item__content) {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.filter-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.bulk-bind-content {
  display: grid;
  gap: 12px;
}

.bulk-bind-hint {
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

.readonly-alert,
.import-error-alert {
  margin-bottom: 12px;
}

.card-header,
.pagination-row {
  align-items: center;
  display: flex;
  justify-content: space-between;
}

.card-header {
  color: #64748b;
  font-size: 13px;
  gap: 12px;
}

.card-title-area {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 10px 16px;
  min-width: 0;
}

.card-title-group {
  align-items: center;
  display: flex;
  gap: 12px;
}

.card-actions {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.card-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.card-header > span:first-child,
.card-title-group span:first-child {
  color: #172033;
  font-size: 16px;
  font-weight: 700;
}

.batch-actions {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.main-cell {
  display: grid;
  gap: 4px;
}

.main-cell strong {
  color: #172033;
}

.main-cell span {
  color: #64748b;
  font-size: 12px;
}

.set-path-list {
  align-items: flex-start;
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.set-path-tag {
  align-items: flex-start;
  height: auto;
  justify-content: flex-start;
  line-height: 1.45;
  max-width: 100%;
  padding-bottom: 3px;
  padding-top: 3px;
  white-space: normal;
}

.set-path-tag :deep(.el-tag__content) {
  min-width: 0;
  white-space: normal;
}

.muted-text {
  color: #94a3b8;
  font-size: 12px;
}

.hierarchy-table .main-cell {
  display: inline-grid;
  vertical-align: top;
}

.hierarchy-table :deep(.el-table__expand-icon) {
  align-items: center;
  display: inline-flex;
  height: 23px;
  justify-content: center;
  margin-right: 8px;
  vertical-align: top;
  width: 23px;
}

.hierarchy-table :deep(.el-table__expand-icon > .el-icon) {
  font-size: 16px;
}

.hierarchy-table :deep(.el-table__placeholder) {
  height: 23px;
  vertical-align: top;
  width: 31px;
}

.media-row-actions {
  align-items: center;
  display: flex;
  gap: 12px;
  width: 100%;
}

.media-row-actions :deep(.el-link),
.media-row-actions :deep(.el-button) {
  margin-left: 0;
  min-width: 0;
  padding-left: 0;
  padding-right: 0;
}

.pagination-row {
  justify-content: flex-end;
  padding-top: 16px;
}

.form-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: 1fr 1fr;
}

.full-input {
  width: 100%;
}

.batch-assignment-alert {
  margin-bottom: 12px;
}

.content-tree-select {
  min-width: 0;
  width: 100%;
}

.content-tree-select :deep(.el-input__wrapper) {
  width: 100%;
}

:global(.content-tree-select-popper) {
  max-width: calc(100vw - 48px);
  min-width: 360px;
}

:global(.content-tree-select-popper .el-select-dropdown__wrap) {
  max-height: 420px;
}

:global(.content-tree-select-popper .el-tree-node__content) {
  min-width: 320px;
  position: relative;
}

:global(.content-tree-select-popper .el-tree-node__label) {
  max-width: 420px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:global(.content-tree-select-popper .el-tree-node__content[style*="padding-left: 18px"]) {
  --tree-branch-x: 9px;
}

:global(.content-tree-select-popper .el-tree-node__content[style*="padding-left: 36px"]) {
  --tree-branch-x: 27px;
}

:global(.content-tree-select-popper .el-tree-node__content[style*="padding-left: 54px"]) {
  --tree-branch-x: 45px;
}

:global(.content-tree-select-popper .el-tree-node__content[style*="padding-left: 72px"]) {
  --tree-branch-x: 63px;
}

:global(.content-tree-select-popper .el-tree-node__content[style*="padding-left: 90px"]) {
  --tree-branch-x: 81px;
}

:global(.content-tree-select-popper .el-tree-node__content[style*="padding-left: 108px"]) {
  --tree-branch-x: 99px;
}

:global(.content-tree-select-popper .el-tree-node__content[style*="padding-left: 126px"]) {
  --tree-branch-x: 117px;
}

:global(.content-tree-select-popper .el-tree-node__content[style*="padding-left: 144px"]) {
  --tree-branch-x: 135px;
}

:global(.content-tree-select-popper .el-tree-node__content[style*="padding-left: 18px"]::before),
:global(.content-tree-select-popper .el-tree-node__content[style*="padding-left: 36px"]::before),
:global(.content-tree-select-popper .el-tree-node__content[style*="padding-left: 54px"]::before),
:global(.content-tree-select-popper .el-tree-node__content[style*="padding-left: 72px"]::before),
:global(.content-tree-select-popper .el-tree-node__content[style*="padding-left: 90px"]::before),
:global(.content-tree-select-popper .el-tree-node__content[style*="padding-left: 108px"]::before),
:global(.content-tree-select-popper .el-tree-node__content[style*="padding-left: 126px"]::before),
:global(.content-tree-select-popper .el-tree-node__content[style*="padding-left: 144px"]::before) {
  border-left: 1px solid #d9e2ef;
  bottom: -4px;
  content: "";
  left: var(--tree-branch-x);
  pointer-events: none;
  position: absolute;
  top: -4px;
}

:global(.content-tree-select-popper .el-tree-node:last-child > .el-tree-node__content::before) {
  bottom: 50%;
}

:global(.content-tree-select-popper .el-tree-node__content[style*="padding-left: 18px"]::after),
:global(.content-tree-select-popper .el-tree-node__content[style*="padding-left: 36px"]::after),
:global(.content-tree-select-popper .el-tree-node__content[style*="padding-left: 54px"]::after),
:global(.content-tree-select-popper .el-tree-node__content[style*="padding-left: 72px"]::after),
:global(.content-tree-select-popper .el-tree-node__content[style*="padding-left: 90px"]::after),
:global(.content-tree-select-popper .el-tree-node__content[style*="padding-left: 108px"]::after),
:global(.content-tree-select-popper .el-tree-node__content[style*="padding-left: 126px"]::after),
:global(.content-tree-select-popper .el-tree-node__content[style*="padding-left: 144px"]::after) {
  border-top: 1px solid #d9e2ef;
  content: "";
  left: var(--tree-branch-x);
  pointer-events: none;
  position: absolute;
  top: 50%;
  width: 12px;
}

.tree-select-node-label {
  display: inline-block;
  max-width: 420px;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: bottom;
  white-space: nowrap;
}

.asset-picker {
  align-items: center;
  display: grid;
  gap: 8px;
  grid-template-columns: minmax(0, 1fr) auto;
  width: 100%;
}

.upload-icon {
  color: #2563eb;
  font-size: 28px;
}

.upload-text {
  color: #64748b;
  font-size: 13px;
}

@media (max-width: 1180px) {
  .list-filter-form,
  .item-filter-form,
  .media-filter-form,
  .set-filter-form,
  .exercise-filter-form,
  .material-filter-form,
  .line-filter-form,
  .line-vocab-filter-form {
    grid-template-columns: 1fr 1fr;
  }

  .filter-actions {
    justify-content: flex-start;
  }

  .filter-actions :deep(.el-form-item__content) {
    justify-content: flex-start;
  }

  .item-filter-form > .el-form-item,
  .exercise-filter-form > .el-form-item {
    flex: 1 1 calc(50% - 6px);
    max-width: none;
  }

  .item-filter-form > .filter-actions,
  .exercise-filter-form > .filter-actions {
    flex: 1 1 100%;
    margin-left: 0;
  }
}

@media (max-width: 720px) {
  .page-heading {
    align-items: flex-start;
    gap: 12px;
  }

  .list-filter-form,
  .item-filter-form,
  .media-filter-form,
  .set-filter-form,
  .exercise-filter-form,
  .material-filter-form,
  .line-filter-form,
  .line-vocab-filter-form,
  .asset-picker,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .item-filter-form > .el-form-item,
  .exercise-filter-form > .el-form-item {
    flex-basis: 100%;
  }
}
</style>
