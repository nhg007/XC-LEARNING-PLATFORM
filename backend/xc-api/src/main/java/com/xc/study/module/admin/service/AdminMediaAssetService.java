package com.xc.study.module.admin.service;

import com.xc.study.common.PageResult;
import com.xc.study.module.admin.dto.AdminMediaAssetQueryDTO;
import com.xc.study.module.admin.dto.AdminUploadMediaAssetDTO;
import com.xc.study.module.admin.vo.AdminMediaAssetVO;
import com.xc.study.security.CurrentUser;

public interface AdminMediaAssetService {

    PageResult<AdminMediaAssetVO> pageAssets(AdminMediaAssetQueryDTO query, CurrentUser admin);

    AdminMediaAssetVO upload(AdminUploadMediaAssetDTO request, CurrentUser admin, String ipAddress);
}
