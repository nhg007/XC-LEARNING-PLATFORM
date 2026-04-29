<template>
  <section class="content-page">
    <div class="page-heading">
      <div>
        <h1>{{ t('content.title') }}</h1>
        <p>{{ t('content.subtitle') }}</p>
      </div>
      <div class="heading-actions">
        <el-button :icon="Download" :loading="templateDownloading" @click="downloadActiveTemplate">
          {{ t('content.actions.downloadTemplate') }}
        </el-button>
        <el-button type="primary" plain :icon="Upload" @click="openCsvImportDialog()">
          {{ t('content.actions.importCsv') }}
        </el-button>
        <el-button :loading="activeLoading" :icon="Refresh" @click="reloadActive">
          {{ t('common.refresh') }}
        </el-button>
      </div>
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
                <el-option :label="t('content.status.active')" value="active" />
                <el-option :label="t('content.status.inactive')" value="inactive" />
              </el-select>
            </el-form-item>
            <el-form-item class="filter-actions">
              <el-button type="primary" :icon="Search" @click="searchLists">{{ t('common.search') }}</el-button>
              <el-button @click="resetListFilters">{{ t('content.reset') }}</el-button>
              <el-button type="primary" plain :icon="Plus" @click="openListDialog()">
                {{ t('content.actions.createList') }}
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" class="table-card">
          <template #header>
            <div class="card-header">
              <span>{{ t('content.listTitle') }}</span>
              <span>{{ t('content.total', { total: listTotal }) }}</span>
            </div>
          </template>
          <el-table v-loading="listLoading" :data="vocabLists" row-key="id" border :empty-text="t('content.emptyLists')">
            <el-table-column prop="id" label="ID" width="84" />
            <el-table-column :label="t('content.columns.vocabList')" min-width="240">
              <template #default="{ row }">
                <div class="main-cell">
                  <strong>{{ row.name }}</strong>
                  <span>{{ row.description || t('common.empty') }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column :label="t('content.columns.type')" width="140">
              <template #default="{ row }">
                {{ t(`content.listTypes.${row.listType}`) }}
              </template>
            </el-table-column>
            <el-table-column prop="level" :label="t('content.columns.level')" width="120" />
            <el-table-column :label="t('content.columns.items')" width="160">
              <template #default="{ row }">
                {{ t('content.itemSummary', { active: row.activeItemCount, inactive: row.inactiveItemCount }) }}
              </template>
            </el-table-column>
            <el-table-column prop="sortOrder" :label="t('content.columns.sortOrder')" width="100" />
            <el-table-column :label="t('content.columns.status')" width="120">
              <template #default="{ row }">
                <el-tag :type="row.status === 'active' ? 'success' : 'info'">
                  {{ t(`content.status.${row.status}`) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="t('content.columns.updatedAt')" min-width="170">
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
              <el-select v-model="itemQuery.vocabListId" clearable filterable :placeholder="t('content.listFilter')">
                <el-option v-for="list in listOptions" :key="list.id" :label="listOptionLabel(list)" :value="list.id" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-select v-model="itemQuery.status" clearable :placeholder="t('content.statusFilter')">
                <el-option :label="t('content.status.active')" value="active" />
                <el-option :label="t('content.status.inactive')" value="inactive" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-select v-model="itemQuery.hasAudio" clearable :placeholder="t('content.audioFilter')">
                <el-option :label="t('content.assetFilters.withAudio')" :value="true" />
                <el-option :label="t('content.assetFilters.missingAudio')" :value="false" />
              </el-select>
            </el-form-item>
            <el-form-item class="filter-actions">
              <el-button type="primary" :icon="Search" @click="searchItems">{{ t('common.search') }}</el-button>
              <el-button @click="resetItemFilters">{{ t('content.reset') }}</el-button>
              <el-button type="primary" plain :icon="Plus" @click="openItemDialog()">
                {{ t('content.actions.createItem') }}
              </el-button>
              <el-button plain :icon="Link" @click="openBulkBindDialog('itemAudio')">
                {{ t('content.actions.bulkBindAudio') }}
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" class="table-card">
          <template #header>
            <div class="card-header">
              <span>{{ t('content.itemTitle') }}</span>
              <span>{{ t('content.total', { total: itemTotal }) }}</span>
            </div>
          </template>
          <el-table v-loading="itemLoading" :data="vocabItems" row-key="id" border :empty-text="t('content.emptyItems')">
            <el-table-column prop="id" label="ID" width="84" />
            <el-table-column :label="t('content.columns.word')" min-width="190">
              <template #default="{ row }">
                <div class="main-cell">
                  <strong>{{ row.hanzi }}</strong>
                  <span>{{ row.pinyin || t('common.empty') }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column :label="t('content.columns.vocabList')" min-width="180">
              <template #default="{ row }">
                {{ row.vocabListName || row.vocabListId }}
              </template>
            </el-table-column>
            <el-table-column prop="meaningEn" :label="t('content.columns.meaningEn')" min-width="180" show-overflow-tooltip />
            <el-table-column prop="meaningRu" :label="t('content.columns.meaningRu')" min-width="180" show-overflow-tooltip />
            <el-table-column :label="t('content.columns.audio')" min-width="130">
              <template #default="{ row }">
                <el-link v-if="row.audioUrl" :href="row.audioUrl" target="_blank" type="primary">#{{ row.audioAssetId }}</el-link>
                <span v-else>{{ t('common.empty') }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="sortOrder" :label="t('content.columns.sortOrder')" width="100" />
            <el-table-column :label="t('content.columns.status')" width="120">
              <template #default="{ row }">
                <el-tag :type="row.status === 'active' ? 'success' : 'info'">
                  {{ t(`content.status.${row.status}`) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="t('content.columns.updatedAt')" min-width="170">
              <template #default="{ row }">
                {{ formatDate(row.updatedAt) }}
              </template>
            </el-table-column>
            <el-table-column :label="t('content.columns.actions')" fixed="right" width="160">
              <template #default="{ row }">
                <el-button link type="primary" @click="openItemDialog(row)">{{ t('content.actions.edit') }}</el-button>
                <el-button link :type="row.status === 'active' ? 'warning' : 'success'" @click="toggleItemStatus(row)">
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
                <el-option :label="t('content.status.active')" value="active" />
                <el-option :label="t('content.status.inactive')" value="inactive" />
              </el-select>
            </el-form-item>
            <el-form-item class="filter-actions">
              <el-button type="primary" :icon="Search" @click="searchMedia">{{ t('common.search') }}</el-button>
              <el-button @click="resetMediaFilters">{{ t('content.reset') }}</el-button>
              <el-button type="primary" plain :icon="Plus" @click="openUploadDialog">
                {{ t('content.actions.uploadMedia') }}
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" class="table-card">
          <template #header>
            <div class="card-header">
              <span>{{ t('content.mediaTitle') }}</span>
              <span>{{ t('content.total', { total: mediaTotal }) }}</span>
            </div>
          </template>
          <el-table v-loading="mediaLoading" :data="mediaAssets" row-key="id" border :empty-text="t('content.emptyMedia')">
            <el-table-column prop="id" label="ID" width="84" />
            <el-table-column :label="t('content.columns.media')" min-width="300">
              <template #default="{ row }">
                <div class="main-cell">
                  <strong>{{ row.url }}</strong>
                  <span>{{ formatFileSize(row.fileSize) }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column :label="t('content.columns.mediaType')" width="120">
              <template #default="{ row }">
                {{ t(`content.mediaTypes.${row.mediaType}`) }}
              </template>
            </el-table-column>
            <el-table-column :label="t('content.columns.language')" width="120">
              <template #default="{ row }">
                {{ row.language ? t(`content.languages.${row.language}`) : t('common.empty') }}
              </template>
            </el-table-column>
            <el-table-column :label="t('content.columns.duration')" width="130">
              <template #default="{ row }">
                {{ formatDurationMs(row.durationMs) }}
              </template>
            </el-table-column>
            <el-table-column :label="t('content.columns.status')" width="110">
              <template #default="{ row }">
                <el-tag :type="row.status === 'active' ? 'success' : 'info'">
                  {{ t(`content.status.${row.status}`) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="t('content.columns.createdAt')" min-width="170">
              <template #default="{ row }">
                {{ formatDate(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column :label="t('content.columns.actions')" fixed="right" width="240">
              <template #default="{ row }">
                <el-link :href="row.url" target="_blank" type="primary">{{ t('content.actions.open') }}</el-link>
                <el-button link :type="row.status === 'active' ? 'warning' : 'success'" @click="toggleMediaStatus(row)">
                  {{ row.status === 'active' ? t('content.actions.disable') : t('content.actions.enable') }}
                </el-button>
                <el-button link type="danger" :disabled="row.status === 'active'" @click="deleteMediaAsset(row)">
                  {{ t('content.actions.delete') }}
                </el-button>
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
                <el-option :label="t('content.status.active')" value="active" />
                <el-option :label="t('content.status.inactive')" value="inactive" />
              </el-select>
            </el-form-item>
            <el-form-item class="filter-actions">
              <el-button type="primary" :icon="Search" @click="searchSets">{{ t('common.search') }}</el-button>
              <el-button @click="resetSetFilters">{{ t('content.reset') }}</el-button>
              <el-button type="primary" plain :icon="Plus" @click="openSetDialog()">
                {{ t('content.actions.createSet') }}
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" class="table-card">
          <template #header>
            <div class="card-header">
              <span>{{ t('content.setTitle') }}</span>
              <span>{{ t('content.total', { total: setTotal }) }}</span>
            </div>
          </template>
          <el-table v-loading="setLoading" :data="exerciseSets" row-key="id" border :empty-text="t('content.emptySets')">
            <el-table-column prop="id" label="ID" width="84" />
            <el-table-column :label="t('content.columns.exerciseSet')" min-width="240">
              <template #default="{ row }">
                <div class="main-cell">
                  <strong>{{ row.title }}</strong>
                  <span>{{ row.level || t('common.empty') }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column :label="t('content.columns.type')" min-width="160">
              <template #default="{ row }">
                {{ t(`content.exerciseTypes.${row.exerciseType}`) }}
              </template>
            </el-table-column>
            <el-table-column :label="t('content.columns.exercises')" width="170">
              <template #default="{ row }">
                {{ t('content.exerciseSummary', { active: row.activeExerciseCount, inactive: row.inactiveExerciseCount }) }}
              </template>
            </el-table-column>
            <el-table-column :label="t('content.columns.status')" width="120">
              <template #default="{ row }">
                <el-tag :type="row.status === 'active' ? 'success' : 'info'">
                  {{ t(`content.status.${row.status}`) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="t('content.columns.updatedAt')" min-width="170">
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
              <el-select v-model="exerciseQuery.exerciseSetId" clearable filterable :placeholder="t('content.fields.exerciseSet')">
                <el-option v-for="set in setOptions" :key="set.id" :label="exerciseSetOptionLabel(set)" :value="set.id" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-select v-model="exerciseQuery.exerciseType" clearable :placeholder="t('content.exerciseTypeFilter')">
                <el-option v-for="type in exerciseTypes" :key="type" :label="t(`content.exerciseTypes.${type}`)" :value="type" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-select v-model="exerciseQuery.status" clearable :placeholder="t('content.statusFilter')">
                <el-option :label="t('content.status.active')" value="active" />
                <el-option :label="t('content.status.inactive')" value="inactive" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-select v-model="exerciseQuery.hasAudio" clearable :placeholder="t('content.audioFilter')">
                <el-option :label="t('content.assetFilters.withAudio')" :value="true" />
                <el-option :label="t('content.assetFilters.missingAudio')" :value="false" />
              </el-select>
            </el-form-item>
            <el-form-item class="filter-actions">
              <el-button type="primary" :icon="Search" @click="searchExercises">{{ t('common.search') }}</el-button>
              <el-button @click="resetExerciseFilters">{{ t('content.reset') }}</el-button>
              <el-button type="primary" plain :icon="Plus" @click="openExerciseDialog()">
                {{ t('content.actions.createExercise') }}
              </el-button>
              <el-button plain :icon="Link" @click="openBulkBindDialog('exerciseAudio')">
                {{ t('content.actions.bulkBindAudio') }}
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" class="table-card">
          <template #header>
            <div class="card-header">
              <span>{{ t('content.exerciseTitle') }}</span>
              <span>{{ t('content.total', { total: exerciseTotal }) }}</span>
            </div>
          </template>
          <el-table v-loading="exerciseLoading" :data="sentenceExercises" row-key="id" border :empty-text="t('content.emptyExercises')">
            <el-table-column prop="id" label="ID" width="84" />
            <el-table-column :label="t('content.columns.answer')" min-width="220">
              <template #default="{ row }">
                <div class="main-cell">
                  <strong>{{ row.hanziAnswer }}</strong>
                  <span>{{ row.pinyinPrompt || row.translationEn || row.translationRu || t('common.empty') }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column :label="t('content.columns.exerciseSet')" min-width="180">
              <template #default="{ row }">
                {{ row.exerciseSetTitle || row.exerciseSetId }}
              </template>
            </el-table-column>
            <el-table-column :label="t('content.columns.type')" min-width="160">
              <template #default="{ row }">
                {{ t(`content.exerciseTypes.${row.exerciseType}`) }}
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
            <el-table-column prop="sortOrder" :label="t('content.columns.sortOrder')" width="100" />
            <el-table-column :label="t('content.columns.status')" width="120">
              <template #default="{ row }">
                <el-tag :type="row.status === 'active' ? 'success' : 'info'">
                  {{ t(`content.status.${row.status}`) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="t('content.columns.updatedAt')" min-width="170">
              <template #default="{ row }">
                {{ formatDate(row.updatedAt) }}
              </template>
            </el-table-column>
            <el-table-column :label="t('content.columns.actions')" fixed="right" width="160">
              <template #default="{ row }">
                <el-button link type="primary" @click="openExerciseDialog(row)">{{ t('content.actions.edit') }}</el-button>
                <el-button link :type="row.status === 'active' ? 'warning' : 'success'" @click="toggleExerciseStatus(row)">
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
              <el-button type="primary" plain :icon="Plus" @click="openMaterialDialog()">
                {{ t('content.actions.createMaterial') }}
              </el-button>
              <el-button plain :icon="Link" @click="openBulkBindDialog('materialCover')">
                {{ t('content.actions.bulkBindCover') }}
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" class="table-card">
          <template #header>
            <div class="card-header">
              <span>{{ t('content.materialTitle') }}</span>
              <span>{{ t('content.total', { total: materialTotal }) }}</span>
            </div>
          </template>
          <el-table v-loading="materialLoading" :data="videoMaterials" row-key="id" border :empty-text="t('content.emptyMaterials')">
            <el-table-column prop="id" label="ID" width="84" />
            <el-table-column :label="t('content.columns.material')" min-width="260">
              <template #default="{ row }">
                <div class="main-cell">
                  <strong>{{ row.title }}</strong>
                  <span>{{ row.description || t('common.empty') }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column :label="t('content.columns.type')" width="140">
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
            <el-table-column prop="lineCount" :label="t('content.columns.lines')" width="100" />
            <el-table-column :label="t('content.columns.status')" width="120">
              <template #default="{ row }">
                <el-tag :type="row.status === 'active' ? 'success' : 'info'">
                  {{ t(`content.status.${row.status}`) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="t('content.columns.updatedAt')" min-width="170">
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
              <el-select v-model="lineQuery.materialId" clearable filterable :placeholder="t('content.materialFilter')">
                <el-option
                  v-for="material in materialOptions"
                  :key="material.id"
                  :label="materialOptionLabel(material)"
                  :value="material.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-select v-model="lineQuery.hasAudio" clearable :placeholder="t('content.audioFilter')">
                <el-option :label="t('content.assetFilters.withAudio')" :value="true" />
                <el-option :label="t('content.assetFilters.missingAudio')" :value="false" />
              </el-select>
            </el-form-item>
            <el-form-item class="filter-actions">
              <el-button type="primary" :icon="Search" @click="searchLines">{{ t('common.search') }}</el-button>
              <el-button @click="resetLineFilters">{{ t('content.reset') }}</el-button>
              <el-button type="primary" plain :icon="Plus" @click="openLineDialog()">
                {{ t('content.actions.createLine') }}
              </el-button>
              <el-button plain :icon="Link" @click="openBulkBindDialog('lineAudio')">
                {{ t('content.actions.bulkBindAudio') }}
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" class="table-card">
          <template #header>
            <div class="card-header">
              <span>{{ t('content.lineTitle') }}</span>
              <span>{{ t('content.total', { total: lineTotal }) }}</span>
            </div>
          </template>
          <el-table v-loading="lineLoading" :data="dialogueLines" row-key="id" border :empty-text="t('content.emptyLines')">
            <el-table-column prop="id" label="ID" width="84" />
            <el-table-column prop="lineNo" :label="t('content.columns.lineNo')" width="90" />
            <el-table-column :label="t('content.columns.material')" min-width="180">
              <template #default="{ row }">
                {{ row.materialTitle || row.materialId }}
              </template>
            </el-table-column>
            <el-table-column :label="t('content.fields.hanziText')" min-width="240">
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
                <el-button link type="primary" @click="openLineDialog(row)">{{ t('content.actions.edit') }}</el-button>
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
              <el-select
                v-model="lineVocabQuery.materialId"
                clearable
                filterable
                :placeholder="t('content.materialFilter')"
                @change="handleLineVocabMaterialFilterChange"
              >
                <el-option
                  v-for="material in materialOptions"
                  :key="material.id"
                  :label="materialOptionLabel(material)"
                  :value="material.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-select v-model="lineVocabQuery.dialogueLineId" clearable filterable :placeholder="t('content.lineFilter')">
                <el-option v-for="line in lineOptions" :key="line.id" :label="lineOptionLabel(line)" :value="line.id" />
              </el-select>
            </el-form-item>
            <el-form-item class="filter-actions">
              <el-button type="primary" :icon="Search" @click="searchLineVocab">{{ t('common.search') }}</el-button>
              <el-button @click="resetLineVocabFilters">{{ t('content.reset') }}</el-button>
              <el-button type="primary" plain :icon="Plus" @click="openLineVocabDialog()">
                {{ t('content.actions.createLineVocab') }}
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" class="table-card">
          <template #header>
            <div class="card-header">
              <span>{{ t('content.lineVocabTitle') }}</span>
              <span>{{ t('content.total', { total: lineVocabTotal }) }}</span>
            </div>
          </template>
          <el-table v-loading="lineVocabLoading" :data="lineVocabRecords" row-key="id" border :empty-text="t('content.emptyLineVocab')">
            <el-table-column prop="id" label="ID" width="84" />
            <el-table-column :label="t('content.columns.word')" min-width="170">
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
                <el-button link type="primary" @click="openLineVocabDialog(row)">{{ t('content.actions.edit') }}</el-button>
                <el-button link type="danger" @click="deleteLineVocab(row)">{{ t('content.actions.delete') }}</el-button>
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
          <el-form-item :label="t('content.fields.level')">
            <el-input v-model="listForm.level" maxlength="20" />
          </el-form-item>
        </div>
        <div class="form-grid">
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
        <el-form-item :label="t('content.fields.vocabList')" prop="vocabListId">
          <el-select v-model="itemForm.vocabListId" class="full-input" filterable>
            <el-option v-for="list in listOptions" :key="list.id" :label="listOptionLabel(list)" :value="list.id" />
          </el-select>
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
        <el-button type="primary" :loading="submitting" @click="submitItem">{{ t('content.submit') }}</el-button>
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
          <el-form-item :label="t('content.fields.level')">
            <el-input v-model="setForm.level" maxlength="20" />
          </el-form-item>
        </div>
        <el-form-item :label="t('content.fields.status')" prop="status">
          <el-radio-group v-model="setForm.status">
            <el-radio-button label="active">{{ t('content.status.active') }}</el-radio-button>
            <el-radio-button label="inactive">{{ t('content.status.inactive') }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
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
        <el-form-item :label="t('content.fields.exerciseSet')" prop="exerciseSetId">
          <el-select v-model="exerciseForm.exerciseSetId" class="full-input" filterable @change="handleExerciseSetChange">
            <el-option v-for="set in setOptions" :key="set.id" :label="exerciseSetOptionLabel(set)" :value="set.id" />
          </el-select>
        </el-form-item>
        <div class="form-grid">
          <el-form-item :label="t('content.fields.exerciseType')" prop="exerciseType">
            <el-select v-model="exerciseForm.exerciseType" class="full-input" disabled>
              <el-option v-for="type in exerciseTypes" :key="type" :label="t(`content.exerciseTypes.${type}`)" :value="type" />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('content.fields.sortOrder')" prop="sortOrder">
            <el-input-number v-model="exerciseForm.sortOrder" class="full-input" :min="0" :max="999999" />
          </el-form-item>
        </div>
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
        <el-button type="primary" :loading="submitting" @click="submitExercise">{{ t('content.submit') }}</el-button>
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
            <el-select v-model="materialForm.materialType" class="full-input">
              <el-option v-for="type in materialTypes" :key="type" :label="t(`content.materialTypes.${type}`)" :value="type" />
            </el-select>
          </el-form-item>
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
        </div>
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
          <el-select v-model="lineForm.materialId" class="full-input" filterable>
            <el-option
              v-for="material in materialOptions"
              :key="material.id"
              :label="materialOptionLabel(material)"
              :value="material.id"
            />
          </el-select>
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
        <el-button type="primary" :loading="submitting" @click="submitLine">{{ t('content.submit') }}</el-button>
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
        <el-button type="primary" :loading="submitting" @click="submitLineVocab">{{ t('content.submit') }}</el-button>
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

    <el-dialog v-model="csvImportDialogVisible" :title="t('content.importCsvTitle')" width="620px">
      <el-form label-position="top">
        <el-form-item :label="t('content.fields.importType')">
          <el-select v-model="csvImportForm.importType" class="full-input">
            <el-option
              v-for="option in contentImportOptions"
              :key="option.value"
              :label="t(`content.importTypes.${option.value}`)"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('content.fields.file')">
          <el-upload
            v-model:file-list="csvImportForm.fileList"
            drag
            accept=".csv,text/csv"
            :auto-upload="false"
            :limit="1"
            :on-exceed="handleCsvImportExceed"
          >
            <el-icon class="upload-icon"><UploadFilled /></el-icon>
            <div class="upload-text">{{ t('content.importDropText') }}</div>
          </el-upload>
        </el-form-item>
        <p class="bulk-bind-hint">{{ t('content.importCsvHint') }}</p>
      </el-form>
      <template #footer>
        <el-button @click="csvImportDialogVisible = false">{{ t('content.cancel') }}</el-button>
        <el-button type="primary" :loading="csvImportSubmitting" @click="submitCsvImport">
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
  updateAdminMediaAssetStatus,
  updateAdminSentenceExercise,
  updateAdminSentenceExerciseStatus,
  updateAdminVideoMaterial,
  updateAdminVideoMaterialStatus,
  updateAdminVocabItem,
  updateAdminVocabItemStatus,
  updateAdminVocabList,
  updateAdminVocabListStatus,
  uploadAdminMediaAsset
} from '@/api/content'
import type {
  AdminBatchBindMediaAssetPayload,
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

const { t, locale } = useI18n()
const route = useRoute()
const vocabTypes: VocabListType[] = ['HSK', 'YCT', 'category', 'professional', 'custom']
const exerciseTypes: ExerciseType[] = ['audio_order', 'audio_dictation', 'pinyin_dictation', 'translation_order']
const materialTypes: VideoMaterialType[] = ['drama', 'short_video', 'cartoon']
type UploadTarget = 'general' | 'itemAudio' | 'exerciseAudio' | 'lineAudio' | 'materialCover'
type BulkBindTarget = 'itemAudio' | 'exerciseAudio' | 'lineAudio' | 'materialCover'
type ContentTab = 'lists' | 'items' | 'media' | 'sets' | 'exercises' | 'materials' | 'lines' | 'lineVocab'

const tabImportTypes: Record<ContentTab, AdminContentImportType | null> = {
  lists: 'vocab-lists',
  items: 'vocab-items',
  media: null,
  sets: 'exercise-sets',
  exercises: 'sentence-exercises',
  materials: 'video-materials',
  lines: 'dialogue-lines',
  lineVocab: 'dialogue-line-vocab'
}

const contentImportOptions: Array<{ value: AdminContentImportType }> = [
  { value: 'vocab-lists' },
  { value: 'vocab-items' },
  { value: 'exercise-sets' },
  { value: 'sentence-exercises' },
  { value: 'video-materials' },
  { value: 'dialogue-lines' },
  { value: 'dialogue-line-vocab' }
]

const activeTab = ref<ContentTab>('lists')
const listLoading = ref(false)
const itemLoading = ref(false)
const mediaLoading = ref(false)
const setLoading = ref(false)
const exerciseLoading = ref(false)
const materialLoading = ref(false)
const lineLoading = ref(false)
const lineVocabLoading = ref(false)
const submitting = ref(false)
const uploading = ref(false)
const bulkBindSubmitting = ref(false)
const csvImportSubmitting = ref(false)
const templateDownloading = ref(false)
const listDialogVisible = ref(false)
const itemDialogVisible = ref(false)
const uploadDialogVisible = ref(false)
const uploadTarget = ref<UploadTarget>('general')
const bulkBindDialogVisible = ref(false)
const csvImportDialogVisible = ref(false)
const setDialogVisible = ref(false)
const exerciseDialogVisible = ref(false)
const materialDialogVisible = ref(false)
const lineDialogVisible = ref(false)
const lineVocabDialogVisible = ref(false)
const vocabLists = ref<AdminVocabList[]>([])
const listOptions = ref<AdminVocabList[]>([])
const vocabItems = ref<AdminVocabItem[]>([])
const vocabItemOptions = ref<AdminVocabItem[]>([])
const mediaAssets = ref<AdminMediaAsset[]>([])
const audioOptions = ref<AdminMediaAsset[]>([])
const imageOptions = ref<AdminMediaAsset[]>([])
const exerciseSets = ref<AdminExerciseSet[]>([])
const setOptions = ref<AdminExerciseSet[]>([])
const sentenceExercises = ref<AdminSentenceExercise[]>([])
const videoMaterials = ref<AdminVideoMaterial[]>([])
const materialOptions = ref<AdminVideoMaterial[]>([])
const dialogueLines = ref<AdminDialogueLine[]>([])
const lineOptions = ref<AdminDialogueLine[]>([])
const lineVocabRecords = ref<AdminDialogueLineVocab[]>([])
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

const activeLoading = computed(() => {
  if (activeTab.value === 'lists') {
    return listLoading.value
  }
  if (activeTab.value === 'items') {
    return itemLoading.value
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
  if (activeTab.value === 'lineVocab') {
    return lineVocabLoading.value
  }
  return mediaLoading.value
})

const activeImportType = computed(() => tabImportTypes[activeTab.value])

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
  status: ''
})

const itemQuery = reactive<AdminVocabItemQuery>({
  page: 1,
  pageSize: 20,
  vocabListId: null,
  keyword: '',
  status: '',
  hasAudio: null
})

const mediaQuery = reactive<AdminMediaAssetQuery>({
  page: 1,
  pageSize: 20,
  keyword: '',
  mediaType: 'audio',
  language: '',
  status: ''
})

const setQuery = reactive<AdminExerciseSetQuery>({
  page: 1,
  pageSize: 20,
  keyword: '',
  exerciseType: '',
  level: '',
  status: ''
})

const exerciseQuery = reactive<AdminSentenceExerciseQuery>({
  page: 1,
  pageSize: 20,
  exerciseSetId: null,
  keyword: '',
  exerciseType: '',
  status: '',
  hasAudio: null
})

const materialQuery = reactive<AdminVideoMaterialQuery>({
  page: 1,
  pageSize: 20,
  keyword: '',
  materialType: '',
  status: '',
  hasCover: null
})

const lineQuery = reactive<AdminDialogueLineQuery>({
  page: 1,
  pageSize: 20,
  materialId: null,
  keyword: '',
  hasAudio: null
})

const lineVocabQuery = reactive<AdminDialogueLineVocabQuery>({
  page: 1,
  pageSize: 20,
  dialogueLineId: null,
  materialId: null,
  keyword: ''
})

const listForm = reactive<{
  name: string
  listType: VocabListType
  level: string
  description: string
  sortOrder: number
  status: ContentStatus
}>({
  name: '',
  listType: 'HSK',
  level: '',
  description: '',
  sortOrder: 0,
  status: 'active'
})

const itemForm = reactive<{
  vocabListId: number | null
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
  fileList: UploadUserFile[]
}>({
  importType: 'vocab-lists',
  fileList: []
})

const setForm = reactive<{
  title: string
  exerciseType: ExerciseType
  level: string
  status: ContentStatus
}>({
  title: '',
  exerciseType: 'audio_order',
  level: '',
  status: 'active'
})

const exerciseForm = reactive<{
  exerciseSetId: number | null
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
  materialType: VideoMaterialType
  description: string
  coverAssetId: number | null
  status: ContentStatus
}>({
  title: '',
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

const listRules = computed<FormRules>(() => ({
  name: [
    { required: true, message: t('content.validation.nameRequired'), trigger: 'blur' },
    { max: 100, message: t('content.validation.nameTooLong'), trigger: 'blur' }
  ],
  listType: [{ required: true, message: t('content.validation.typeRequired'), trigger: 'change' }],
  sortOrder: [{ required: true, message: t('content.validation.sortRequired'), trigger: 'change' }]
}))

const itemRules = computed<FormRules>(() => ({
  vocabListId: [{ required: true, message: t('content.validation.listRequired'), trigger: 'change' }],
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
  exerciseSetId: [{ required: true, message: t('content.validation.exerciseSetRequired'), trigger: 'change' }],
  exerciseType: [{ required: true, message: t('content.validation.typeRequired'), trigger: 'change' }],
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

async function loadLists() {
  listLoading.value = true
  try {
    const result = await fetchAdminVocabLists(listQuery)
    vocabLists.value = result.records
    listTotal.value = result.total
  } finally {
    listLoading.value = false
  }
}

async function loadListOptions() {
  const result = await fetchAdminVocabLists({ page: 1, pageSize: 100, keyword: '', listType: '', level: '', status: '' })
  listOptions.value = result.records
}

async function loadItems() {
  itemLoading.value = true
  try {
    const result = await fetchAdminVocabItems(itemQuery)
    vocabItems.value = result.records
    itemTotal.value = result.total
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
    const result = await fetchAdminExerciseSets(setQuery)
    exerciseSets.value = result.records
    setTotal.value = result.total
  } finally {
    setLoading.value = false
  }
}

async function loadExerciseSetOptions() {
  const result = await fetchAdminExerciseSets({ page: 1, pageSize: 100, keyword: '', exerciseType: '', level: '', status: '' })
  setOptions.value = result.records
}

async function loadSentenceExercises() {
  exerciseLoading.value = true
  try {
    const result = await fetchAdminSentenceExercises(exerciseQuery)
    sentenceExercises.value = result.records
    exerciseTotal.value = result.total
  } finally {
    exerciseLoading.value = false
  }
}

async function loadVideoMaterials() {
  materialLoading.value = true
  try {
    const result = await fetchAdminVideoMaterials(materialQuery)
    videoMaterials.value = result.records
    materialTotal.value = result.total
  } finally {
    materialLoading.value = false
  }
}

async function loadMaterialOptions() {
  const result = await fetchAdminVideoMaterials({ page: 1, pageSize: 100, keyword: '', materialType: '', status: '' })
  materialOptions.value = result.records
}

async function loadDialogueLines() {
  lineLoading.value = true
  try {
    const result = await fetchAdminDialogueLines(lineQuery)
    dialogueLines.value = result.records
    lineTotal.value = result.total
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
  } finally {
    lineVocabLoading.value = false
  }
}

function reloadActive() {
  if (activeTab.value === 'lists') {
    void loadLists()
    return
  }
  if (activeTab.value === 'items') {
    void loadItems()
    return
  }
  if (activeTab.value === 'sets') {
    void loadExerciseSets()
    return
  }
  if (activeTab.value === 'exercises') {
    void loadSentenceExercises()
    return
  }
  if (activeTab.value === 'materials') {
    void loadVideoMaterials()
    return
  }
  if (activeTab.value === 'lines') {
    void loadDialogueLines()
    return
  }
  if (activeTab.value === 'lineVocab') {
    void loadLineVocab()
    return
  }
  void loadMediaAssets()
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

function searchLists() {
  listQuery.page = 1
  void loadLists()
}

function resetListFilters() {
  listQuery.keyword = ''
  listQuery.listType = ''
  listQuery.level = ''
  listQuery.status = ''
  listQuery.page = 1
  void loadLists()
}

function handleListPageSizeChange() {
  listQuery.page = 1
  void loadLists()
}

function searchItems() {
  itemQuery.page = 1
  void loadItems()
}

function resetItemFilters() {
  itemQuery.keyword = ''
  itemQuery.vocabListId = null
  itemQuery.status = ''
  itemQuery.hasAudio = null
  itemQuery.page = 1
  void loadItems()
}

function handleItemPageSizeChange() {
  itemQuery.page = 1
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
  mediaQuery.status = ''
  mediaQuery.page = 1
  void loadMediaAssets()
}

function handleMediaPageSizeChange() {
  mediaQuery.page = 1
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
  setQuery.status = ''
  setQuery.page = 1
  void loadExerciseSets()
}

function handleSetPageSizeChange() {
  setQuery.page = 1
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
  exerciseQuery.status = ''
  exerciseQuery.hasAudio = null
  exerciseQuery.page = 1
  void loadSentenceExercises()
}

function handleExercisePageSizeChange() {
  exerciseQuery.page = 1
  void loadSentenceExercises()
}

function searchMaterials() {
  materialQuery.page = 1
  void loadVideoMaterials()
}

function resetMaterialFilters() {
  materialQuery.keyword = ''
  materialQuery.materialType = ''
  materialQuery.status = ''
  materialQuery.hasCover = null
  materialQuery.page = 1
  void loadVideoMaterials()
}

function handleMaterialPageSizeChange() {
  materialQuery.page = 1
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
  listForm.listType = list?.listType || 'HSK'
  listForm.level = list?.level || ''
  listForm.description = list?.description || ''
  listForm.sortOrder = list?.sortOrder || 0
  listForm.status = list?.status || 'active'
  listDialogVisible.value = true
}

function openItemDialog(item?: AdminVocabItem) {
  editingItem.value = item || null
  itemForm.vocabListId = item?.vocabListId || itemQuery.vocabListId || listOptions.value[0]?.id || null
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
  setForm.exerciseType = set?.exerciseType || 'audio_order'
  setForm.level = set?.level || ''
  setForm.status = set?.status || 'active'
  setDialogVisible.value = true
}

function openExerciseDialog(exercise?: AdminSentenceExercise) {
  editingExercise.value = exercise || null
  const initialSetId = exercise?.exerciseSetId || exerciseQuery.exerciseSetId || setOptions.value[0]?.id || null
  const initialSet = setOptions.value.find(item => item.id === initialSetId)
  exerciseForm.exerciseSetId = initialSetId
  exerciseForm.exerciseType = exercise?.exerciseType || initialSet?.exerciseType || 'audio_order'
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

function handleExerciseSetChange(setId: number) {
  const selected = setOptions.value.find(item => item.id === setId)
  if (selected) {
    exerciseForm.exerciseType = selected.exerciseType
  }
}

function openMaterialDialog(material?: AdminVideoMaterial) {
  editingMaterial.value = material || null
  materialForm.title = material?.title || ''
  materialForm.materialType = material?.materialType || 'drama'
  materialForm.description = material?.description || ''
  materialForm.coverAssetId = material?.coverAssetId || null
  materialForm.status = material?.status || 'active'
  materialDialogVisible.value = true
  if (imageOptions.value.length === 0) {
    void loadImageOptions()
  }
}

function openLineDialog(line?: AdminDialogueLine) {
  editingLine.value = line || null
  lineForm.materialId = line?.materialId || lineQuery.materialId || materialOptions.value[0]?.id || null
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
  lineVocabForm.dialogueLineId = record?.dialogueLineId || lineVocabQuery.dialogueLineId || lineOptions.value[0]?.id || null
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
  bulkBindForm.target = target
  bulkBindForm.rowsText = ''
  bulkBindDialogVisible.value = true
  if (target === 'materialCover') {
    void loadImageOptions()
    return
  }
  void loadAudioOptions()
}

function openCsvImportDialog(importType: AdminContentImportType | null = activeImportType.value) {
  if (!importType) {
    return
  }
  csvImportForm.importType = importType
  csvImportForm.fileList = []
  csvImportDialogVisible.value = true
}

async function submitList() {
  await listFormRef.value?.validate()
  submitting.value = true
  try {
    const payload = {
      name: listForm.name.trim(),
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
    await loadLists()
    await loadListOptions()
  } finally {
    submitting.value = false
  }
}

async function submitItem() {
  await itemFormRef.value?.validate()
  if (!itemForm.vocabListId) {
    return
  }
  submitting.value = true
  try {
    const payload = {
      vocabListId: itemForm.vocabListId,
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
    await loadExerciseSets()
    await loadExerciseSetOptions()
  } finally {
    submitting.value = false
  }
}

async function submitExercise() {
  await exerciseFormRef.value?.validate()
  if (!exerciseForm.exerciseSetId) {
    return
  }
  submitting.value = true
  try {
    const payload = {
      exerciseSetId: exerciseForm.exerciseSetId,
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
    await loadVideoMaterials()
    await loadMaterialOptions()
  } finally {
    submitting.value = false
  }
}

async function submitLine() {
  await lineFormRef.value?.validate()
  if (!lineForm.materialId) {
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

async function downloadActiveTemplate() {
  if (!activeImportType.value) {
    ElMessage.warning(t('content.importTemplateUnavailable'))
    return
  }
  await downloadTemplate(activeImportType.value)
}

async function downloadTemplate(importType: AdminContentImportType) {
  templateDownloading.value = true
  try {
    const template = await downloadAdminContentImportTemplate(importType)
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
  if (!file) {
    ElMessage.warning(t('content.validation.fileRequired'))
    return
  }
  const formData = new FormData()
  formData.append('file', file)
  csvImportSubmitting.value = true
  try {
    const result = await importAdminContentCsv(csvImportForm.importType, formData)
    csvImportDialogVisible.value = false
    await reloadImportType(csvImportForm.importType)
    const summary = t('content.importResult', {
      success: result.successCount,
      requested: result.requestedCount
    })
    if (result.errors.length > 0) {
      await ElMessageBox.alert([summary, ...result.errors].join('\n'), t('content.importResultTitle'), {
        confirmButtonText: t('content.submit'),
        type: result.successCount > 0 ? 'warning' : 'error'
      })
      return
    }
    ElMessage.success(summary)
  } finally {
    csvImportSubmitting.value = false
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
  await loadLists()
  await loadListOptions()
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
  await loadExerciseSets()
  await loadExerciseSetOptions()
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
  await loadVideoMaterials()
  await loadMaterialOptions()
}

function listOptionLabel(list: AdminVocabList) {
  return `${list.name}${list.level ? ` / ${list.level}` : ''}`
}

function exerciseSetOptionLabel(set: AdminExerciseSet) {
  return `${set.title}${set.level ? ` / ${set.level}` : ''} / ${t(`content.exerciseTypes.${set.exerciseType}`)}`
}

function materialOptionLabel(material: AdminVideoMaterial) {
  return `${material.title} / ${t(`content.materialTypes.${material.materialType}`)}`
}

function lineOptionLabel(line: AdminDialogueLine) {
  return `#${line.lineNo} ${line.hanziText}`
}

function vocabItemOptionLabel(item: AdminVocabItem) {
  return `${item.hanzi}${item.pinyin ? ` / ${item.pinyin}` : ''}`
}

function mediaOptionLabel(asset: AdminMediaAsset) {
  return `#${asset.id} ${asset.language ? t(`content.languages.${asset.language}`) : ''} ${asset.url}`
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
  await loadVideoMaterials()
  await loadMaterialOptions()
}

async function reloadImportType(importType: AdminContentImportType) {
  if (importType === 'vocab-lists') {
    await loadLists()
    await loadListOptions()
    return
  }
  if (importType === 'vocab-items') {
    await loadItems()
    await loadVocabItemOptions()
    return
  }
  if (importType === 'exercise-sets') {
    await loadExerciseSets()
    await loadExerciseSetOptions()
    return
  }
  if (importType === 'sentence-exercises') {
    await loadSentenceExercises()
    return
  }
  if (importType === 'video-materials') {
    await loadVideoMaterials()
    await loadMaterialOptions()
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
    listQuery.status = isContentStatus(status) ? status : ''
  }

  if (activeTab.value === 'items') {
    itemQuery.page = page
    itemQuery.pageSize = pageSize
    itemQuery.vocabListId = routeNumber('vocabListId', 0) || null
    itemQuery.keyword = routeText('keyword')
    itemQuery.status = isContentStatus(status) ? status : ''
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
    mediaQuery.status = isContentStatus(status) ? status : ''
  }

  if (activeTab.value === 'sets') {
    const exerciseType = routeText('exerciseType')
    setQuery.page = page
    setQuery.pageSize = pageSize
    setQuery.keyword = routeText('keyword')
    setQuery.exerciseType = isExerciseType(exerciseType) ? exerciseType : ''
    setQuery.level = routeText('level')
    setQuery.status = isContentStatus(status) ? status : ''
  }

  if (activeTab.value === 'exercises') {
    const exerciseType = routeText('exerciseType')
    exerciseQuery.page = page
    exerciseQuery.pageSize = pageSize
    exerciseQuery.exerciseSetId = routeNumber('exerciseSetId', 0) || null
    exerciseQuery.keyword = routeText('keyword')
    exerciseQuery.exerciseType = isExerciseType(exerciseType) ? exerciseType : ''
    exerciseQuery.status = isContentStatus(status) ? status : ''
    exerciseQuery.hasAudio = routeBoolean('hasAudio')
  }

  if (activeTab.value === 'materials') {
    const materialType = routeText('materialType')
    materialQuery.page = page
    materialQuery.pageSize = pageSize
    materialQuery.keyword = routeText('keyword')
    materialQuery.materialType = isVideoMaterialType(materialType) ? materialType : ''
    materialQuery.status = isContentStatus(status) ? status : ''
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

.heading-actions {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
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

.admin-tabs :deep(.el-tabs__header) {
  margin-bottom: 14px;
}

.filter-card :deep(.el-card__body) {
  padding-bottom: 0;
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
  grid-template-columns: minmax(240px, 320px) 220px 130px 140px auto;
}

.media-filter-form {
  grid-template-columns: minmax(260px, 360px) 150px 150px auto;
}

.set-filter-form {
  grid-template-columns: minmax(240px, 320px) 180px 130px 150px auto;
}

.exercise-filter-form {
  grid-template-columns: minmax(220px, 300px) 200px 160px 130px 140px auto;
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

.bulk-bind-content {
  display: grid;
  gap: 12px;
}

.bulk-bind-hint {
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
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
}

.card-header span:first-child {
  color: #172033;
  font-size: 16px;
  font-weight: 700;
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
}
</style>
