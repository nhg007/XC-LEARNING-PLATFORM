package com.xc.study.module.admin.service;

import com.xc.study.common.PageResult;
import com.xc.study.module.admin.dto.AdminBatchBindMediaAssetDTO;
import com.xc.study.module.admin.dto.AdminBatchUpdateContentStatusDTO;
import com.xc.study.module.admin.dto.AdminUpdateContentStatusDTO;
import com.xc.study.module.admin.dto.AdminUpsertVocabItemDTO;
import com.xc.study.module.admin.dto.AdminUpsertVocabListDTO;
import com.xc.study.module.admin.dto.AdminVocabItemQueryDTO;
import com.xc.study.module.admin.dto.AdminVocabListQueryDTO;
import com.xc.study.module.admin.vo.AdminBatchBindMediaAssetResultVO;
import com.xc.study.module.admin.vo.AdminBatchContentStatusResultVO;
import com.xc.study.module.admin.vo.AdminVocabItemVO;
import com.xc.study.module.admin.vo.AdminVocabListVO;
import com.xc.study.security.CurrentUser;

public interface AdminVocabManagementService {

    PageResult<AdminVocabListVO> pageLists(AdminVocabListQueryDTO query, CurrentUser admin);

    AdminVocabListVO createList(AdminUpsertVocabListDTO request, CurrentUser admin, String ipAddress);

    AdminVocabListVO updateList(Long listId, AdminUpsertVocabListDTO request, CurrentUser admin, String ipAddress);

    AdminVocabListVO updateListStatus(Long listId, AdminUpdateContentStatusDTO request, CurrentUser admin, String ipAddress);

    AdminBatchContentStatusResultVO updateListStatuses(AdminBatchUpdateContentStatusDTO request, CurrentUser admin, String ipAddress);

    PageResult<AdminVocabItemVO> pageItems(AdminVocabItemQueryDTO query, CurrentUser admin);

    AdminVocabItemVO createItem(AdminUpsertVocabItemDTO request, CurrentUser admin, String ipAddress);

    AdminVocabItemVO updateItem(Long itemId, AdminUpsertVocabItemDTO request, CurrentUser admin, String ipAddress);

    AdminVocabItemVO updateItemStatus(Long itemId, AdminUpdateContentStatusDTO request, CurrentUser admin, String ipAddress);

    AdminBatchContentStatusResultVO updateItemStatuses(AdminBatchUpdateContentStatusDTO request, CurrentUser admin, String ipAddress);

    AdminBatchBindMediaAssetResultVO bindItemAudio(AdminBatchBindMediaAssetDTO request, CurrentUser admin, String ipAddress);
}
