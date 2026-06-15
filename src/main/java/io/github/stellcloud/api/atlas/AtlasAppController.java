package io.github.stellcloud.api.atlas;

import io.github.stellcloud.api.atlas.vo.ApplicationDeleteVO;
import io.github.stellcloud.api.atlas.vo.ApplicationListVO;
import io.github.stellcloud.api.atlas.vo.ApplicationRequestVO;
import io.github.stellcloud.api.atlas.vo.ApplicationVO;
import io.github.stellcloud.infrastructure.atlas.AtlasControlPlaneClient;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** CMDB 应用页面接口。 */
@Validated
@RestController
@RequestMapping("/api/stellcloud/control-plane/v1/atlas")
public class AtlasAppController {

    private final AtlasControlPlaneClient atlasControlPlaneClient;

    public AtlasAppController(AtlasControlPlaneClient atlasControlPlaneClient) {
        this.atlasControlPlaneClient = atlasControlPlaneClient;
    }

    /** 查询 CMDB 应用列表。 */
    @GetMapping("/apps")
    public ApplicationListVO listApplications(
            @RequestParam(name = "environment", required = false) String environment,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "offset", required = false) Integer offset) {
        return atlasControlPlaneClient.listApplications(environment, status, search, limit, offset);
    }

    /** 查询 CMDB 应用详情。 */
    @GetMapping("/apps/{appId}")
    public ApplicationVO getApplication(@PathVariable("appId") @NotBlank String appId) {
        return atlasControlPlaneClient.getApplication(appId);
    }

    /** 创建 CMDB 应用。 */
    @PostMapping("/apps")
    public ApplicationVO createApplication(@Valid @RequestBody ApplicationRequestVO request) {
        return atlasControlPlaneClient.createApplication(request);
    }

    /** 更新 CMDB 应用。 */
    @PutMapping("/apps/{appId}")
    public ApplicationVO updateApplication(
            @PathVariable("appId") @NotBlank String appId,
            @Valid @RequestBody ApplicationRequestVO request) {
        return atlasControlPlaneClient.updateApplication(appId, request);
    }

    /** 删除 CMDB 应用。 */
    @DeleteMapping("/apps/{appId}")
    public ApplicationDeleteVO deleteApplication(@PathVariable("appId") @NotBlank String appId) {
        return atlasControlPlaneClient.deleteApplication(appId);
    }
}
