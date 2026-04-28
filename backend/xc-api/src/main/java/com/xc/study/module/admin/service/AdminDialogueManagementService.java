package com.xc.study.module.admin.service;

import com.xc.study.common.PageResult;
import com.xc.study.module.admin.dto.AdminDialogueLineQueryDTO;
import com.xc.study.module.admin.dto.AdminDialogueLineVocabQueryDTO;
import com.xc.study.module.admin.dto.AdminUpdateContentStatusDTO;
import com.xc.study.module.admin.dto.AdminUpsertDialogueLineDTO;
import com.xc.study.module.admin.dto.AdminUpsertDialogueLineVocabDTO;
import com.xc.study.module.admin.dto.AdminUpsertVideoMaterialDTO;
import com.xc.study.module.admin.dto.AdminVideoMaterialQueryDTO;
import com.xc.study.module.admin.vo.AdminDialogueLineVO;
import com.xc.study.module.admin.vo.AdminDialogueLineVocabVO;
import com.xc.study.module.admin.vo.AdminVideoMaterialVO;
import com.xc.study.security.CurrentUser;

public interface AdminDialogueManagementService {

    PageResult<AdminVideoMaterialVO> pageMaterials(AdminVideoMaterialQueryDTO query, CurrentUser admin);

    AdminVideoMaterialVO createMaterial(AdminUpsertVideoMaterialDTO request, CurrentUser admin, String ipAddress);

    AdminVideoMaterialVO updateMaterial(Long materialId, AdminUpsertVideoMaterialDTO request, CurrentUser admin, String ipAddress);

    AdminVideoMaterialVO updateMaterialStatus(Long materialId, AdminUpdateContentStatusDTO request, CurrentUser admin, String ipAddress);

    PageResult<AdminDialogueLineVO> pageLines(AdminDialogueLineQueryDTO query, CurrentUser admin);

    AdminDialogueLineVO createLine(AdminUpsertDialogueLineDTO request, CurrentUser admin, String ipAddress);

    AdminDialogueLineVO updateLine(Long lineId, AdminUpsertDialogueLineDTO request, CurrentUser admin, String ipAddress);

    PageResult<AdminDialogueLineVocabVO> pageLineVocab(AdminDialogueLineVocabQueryDTO query, CurrentUser admin);

    AdminDialogueLineVocabVO createLineVocab(AdminUpsertDialogueLineVocabDTO request, CurrentUser admin, String ipAddress);

    AdminDialogueLineVocabVO updateLineVocab(Long vocabId, AdminUpsertDialogueLineVocabDTO request, CurrentUser admin, String ipAddress);

    void deleteLineVocab(Long vocabId, CurrentUser admin, String ipAddress);
}
