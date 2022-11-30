package com.vikadata.api.workspace.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import com.vikadata.api.base.enums.ActionException;
import com.vikadata.api.base.enums.ParameterException;
import com.vikadata.api.organization.dto.MemberDTO;
import com.vikadata.api.organization.vo.CreatedMemberInfoVo;
import com.vikadata.api.organization.vo.MemberBriefInfoVo;
import com.vikadata.api.shared.component.scanner.annotation.ApiResource;
import com.vikadata.api.shared.component.scanner.annotation.GetResource;
import com.vikadata.api.shared.component.notification.annotation.Notification;
import com.vikadata.api.shared.component.scanner.annotation.PostResource;
import com.vikadata.api.shared.cache.bean.OpenedSheet;
import com.vikadata.api.shared.cache.service.SpaceCapacityCacheService;
import com.vikadata.api.shared.cache.service.UserActiveSpaceService;
import com.vikadata.api.shared.cache.service.UserSpaceOpenedSheetService;
import com.vikadata.api.shared.component.notification.NotificationTemplateId;
import com.vikadata.api.shared.config.properties.LimitProperties;
import com.vikadata.api.shared.constants.AuditConstants;
import com.vikadata.api.shared.constants.FileSuffixConstants;
import com.vikadata.api.shared.constants.ParamsConstants;
import com.vikadata.api.shared.context.LoginContext;
import com.vikadata.api.shared.context.SessionContext;
import com.vikadata.api.enterprise.control.infrastructure.ControlRoleDict;
import com.vikadata.api.enterprise.control.infrastructure.ControlTemplate;
import com.vikadata.api.enterprise.control.infrastructure.permission.NodePermission;
import com.vikadata.api.enterprise.control.infrastructure.role.ControlRole;
import com.vikadata.api.enterprise.control.infrastructure.role.ControlRoleManager;
import com.vikadata.api.enterprise.control.infrastructure.role.RoleConstants.Node;
import com.vikadata.api.space.enums.AuditSpaceAction;
import com.vikadata.api.workspace.enums.PermissionException;
import com.vikadata.api.shared.listener.event.AuditSpaceEvent;
import com.vikadata.api.shared.listener.event.AuditSpaceEvent.AuditSpaceArg;
import com.vikadata.api.shared.holder.SpaceHolder;
import com.vikadata.api.workspace.ro.ActiveSheetsOpRo;
import com.vikadata.api.workspace.ro.RemindMemberRo;
import com.vikadata.api.workspace.ro.RemindUnitsNoPermissionRo;
import com.vikadata.api.workspace.ro.ImportExcelOpRo;
import com.vikadata.api.workspace.ro.NodeCopyOpRo;
import com.vikadata.api.workspace.ro.NodeDescOpRo;
import com.vikadata.api.workspace.ro.NodeMoveOpRo;
import com.vikadata.api.workspace.ro.NodeOpRo;
import com.vikadata.api.workspace.ro.NodeUpdateOpRo;
import com.vikadata.api.workspace.ro.VikaBundleOpRo;
import com.vikadata.api.workspace.vo.NodeInfo;
import com.vikadata.api.workspace.vo.NodeInfoTreeVo;
import com.vikadata.api.workspace.vo.NodeInfoVo;
import com.vikadata.api.workspace.vo.NodeInfoWindowVo;
import com.vikadata.api.workspace.vo.NodePathVo;
import com.vikadata.api.workspace.vo.NodePermissionView;
import com.vikadata.api.workspace.vo.NodeSearchResult;
import com.vikadata.api.workspace.vo.ShowcaseVo;
import com.vikadata.api.workspace.vo.ShowcaseVo.NodeExtra;
import com.vikadata.api.workspace.vo.ShowcaseVo.Social;
import com.vikadata.api.organization.mapper.MemberMapper;
import com.vikadata.api.organization.service.IMemberService;
import com.vikadata.api.organization.service.IUnitService;
import com.vikadata.api.space.enums.SpaceException;
import com.vikadata.api.user.mapper.UserMapper;
import com.vikadata.api.workspace.mapper.NodeDescMapper;
import com.vikadata.api.workspace.mapper.NodeFavoriteMapper;
import com.vikadata.api.workspace.mapper.NodeShareSettingMapper;
import com.vikadata.api.workspace.model.NodeCopyEffectDTO;
import com.vikadata.api.workspace.service.IDatasheetService;
import com.vikadata.api.workspace.service.INodeDescService;
import com.vikadata.api.workspace.service.INodeRelService;
import com.vikadata.api.workspace.service.INodeService;
import com.vikadata.api.workspace.service.VikaBundleService;
import com.vikadata.api.shared.util.information.InformationUtil;
import com.vikadata.api.workspace.enums.NodeException;
import com.vikadata.api.workspace.enums.NodeType;
import com.vikadata.core.util.SpringContextHolder;
import com.vikadata.core.exception.BusinessException;
import com.vikadata.core.support.ResponseData;
import com.vikadata.core.util.ExceptionUtil;
import com.vikadata.core.util.SqlTool;
import com.vikadata.core.util.FileTool;
import com.vikadata.entity.NodeEntity;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@Api(tags = "Workbench - Node Api")
@RestController
@ApiResource(path = "/node")
@Slf4j
public class NodeController {

    @Resource
    private IMemberService memberService;

    @Resource
    private INodeService iNodeService;

    @Resource
    private INodeDescService iNodeDescService;

    @Resource
    private NodeDescMapper nodeDescMapper;

    @Resource
    private NodeFavoriteMapper nodeFavoriteMapper;

    @Resource
    private VikaBundleService vikaBundleService;

    @Resource
    private IUnitService unitService;

    @Resource
    private ControlTemplate controlTemplate;

    @Resource
    private NodeShareSettingMapper nodeShareSettingMapper;

    @Resource
    private UserSpaceOpenedSheetService userSpaceOpenedSheetService;

    @Resource
    private IDatasheetService datasheetService;

    @Resource
    private UserMapper userMapper;

    @Resource
    private MemberMapper memberMapper;

    @Resource
    private INodeRelService iNodeRelService;

    @Resource
    private LimitProperties limitProperties;

    @Resource
    private SpaceCapacityCacheService spaceCapacityCacheService;

    @Resource
    private UserActiveSpaceService userActiveSpaceService;

    private static final String ROLE_DESC = "<br/>Role Type：<br/>" +
            "1.owner can add, edit, move, sort, delete, copy folders in the specified working directory。<br/>" +
            "2.manager can add, edit, move, sort, delete, and copy folders in the specified working directory.<br/>" +
            "3.editor can only edit records and views of the data table, but not edit fields<br/>" +
            "4.readonly can only view the number table, you cannot make any edits and modifications, you can only assign read-only permissions to other members。<br/>";

    @GetResource(path = "/search")
    @ApiOperation(value = "Fuzzy search node", notes = "Enter the search term to search for the node of the working directory." + ROLE_DESC)
    @ApiImplicitParams({
            @ApiImplicitParam(name = ParamsConstants.SPACE_ID, value = "space id", required = true, dataTypeClass = String.class, paramType = "header", example = "spcyQkKp9XJEl"),
            @ApiImplicitParam(name = "className", value = "highlight style", dataTypeClass = String.class, paramType = "query", example = "highLight"),
            @ApiImplicitParam(name = "keyword", value = "keyword", required = true, dataTypeClass = String.class, paramType = "query", example = "datasheet")
    })
    public ResponseData<List<NodeSearchResult>> searchNode(@RequestParam(name = "keyword") String keyword,
            @RequestParam(value = "className", required = false, defaultValue = "keyword") String className) {
        String spaceId = LoginContext.me().getSpaceId();
        Long memberId = LoginContext.me().getMemberId();
        List<NodeSearchResult> nodeInfos = iNodeService.searchNode(spaceId, memberId, keyword);
        nodeInfos.forEach(info -> info.setNodeName(InformationUtil.keywordHighlight(info.getNodeName(), keyword, className)));
        return ResponseData.success(nodeInfos);
    }

    @GetResource(path = "/tree")
    @ApiOperation(value = "Query tree node", notes = "Query the node tree of workbench, restricted to two levels." + ROLE_DESC)
    @ApiImplicitParams({
            @ApiImplicitParam(name = ParamsConstants.SPACE_ID, value = "space id", required = true, dataTypeClass = String.class, paramType = "header", example = "spcyQkKp9XJEl"),
            @ApiImplicitParam(name = "depth", value = "tree depth, we can specify the query depth, maximum 2 layers depth.", dataTypeClass = Integer.class, paramType = "query", example = "2")
    })
    public ResponseData<NodeInfoTreeVo> getTree(@RequestParam(name = "depth", defaultValue = "2") @Valid @Min(0) @Max(2) Integer depth) {
        String spaceId = LoginContext.me().getSpaceId();
        Long memberId = LoginContext.me().getMemberId();
        String rootNodeId = iNodeService.getRootNodeIdBySpaceId(spaceId);
        NodeInfoTreeVo tree = iNodeService.getNodeTree(spaceId, rootNodeId, memberId, depth);
        return ResponseData.success(tree);
    }

    @GetResource(path = "/list")
    @ApiOperation(value = "Get nodes of the specified type", notes = "scenario: query an existing dashboard")
    @ApiImplicitParams({
            @ApiImplicitParam(name = ParamsConstants.SPACE_ID, value = "space id", required = true, paramType = "header", dataTypeClass = String.class, example = "spczJrh2i3tLW"),
            @ApiImplicitParam(name = "type", value = "node type", required = true, dataTypeClass = Integer.class, paramType = "query", example = "2"),
            @ApiImplicitParam(name = "role", value = "role（manageable by default）", dataTypeClass = String.class, paramType = "query", example = "manager")
    })
    public ResponseData<List<NodeInfo>> list(@RequestParam(value = "type") Integer type, @RequestParam(value = "role", required = false, defaultValue = "manager") String role) {
        String spaceId = LoginContext.me().getSpaceId();
        Long memberId = LoginContext.me().getMemberId();
        List<String> nodeIds = iNodeService.getNodeIdBySpaceIdAndType(spaceId, type);
        if (nodeIds.isEmpty()) {
            return ResponseData.success(new ArrayList<>());
        }
        ControlRoleDict roleDict = controlTemplate.fetchNodeRole(memberId, nodeIds);
        if (roleDict.isEmpty()) {
            return ResponseData.success(new ArrayList<>());
        }
        ControlRole requireRole = ControlRoleManager.parseNodeRole(role);
        List<String> filterNodeIds = roleDict.entrySet().stream()
                .filter(entry -> entry.getValue().isGreaterThanOrEqualTo(requireRole))
                .map(Map.Entry::getKey).collect(Collectors.toList());
        if (CollUtil.isEmpty(filterNodeIds)) {
            return ResponseData.success(new ArrayList<>());
        }
        return ResponseData.success(iNodeService.getNodeInfoByNodeIds(filterNodeIds));
    }

    @GetResource(path = "/get", requiredPermission = false)
    @ApiOperation(value = "Query nodes", notes = "obtain information about the node " + ROLE_DESC)
    @ApiImplicitParam(name = "nodeIds", value = "node ids", required = true, dataTypeClass = String.class, paramType = "query", example = "nodRTGSy43DJ9,nodRTGSy43DJ9")
    public ResponseData<List<NodeInfoVo>> getByNodeId(@RequestParam("nodeIds") List<String> nodeIds) {
        // Obtain the space ID. The method includes determining whether the node exists.
        String spaceId = iNodeService.getSpaceIdByNodeId(nodeIds.get(0));
        // Gets the member ID by determining whether the user is in this space.
        Long memberId = LoginContext.me().getUserSpaceDto(spaceId).getMemberId();
        return ResponseData.success(iNodeService.getNodeInfoByNodeIds(spaceId, memberId, nodeIds));
    }

    @GetResource(path = "/showcase", requiredLogin = false)
    @ApiOperation(value = "Folder preview", notes = "Nodes that are not in the center of the template, make cross-space judgments.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "nodeId", value = "node id", required = true, dataTypeClass = String.class, paramType = "query", example = "nodRTGSy43DJ9"),
            @ApiImplicitParam(name = "shareId", value = "share id", dataTypeClass = String.class, paramType = "query", example = "shrRTGSy43DJ9")
    })
    public ResponseData<ShowcaseVo> showcase(@RequestParam("nodeId") String nodeId,
            @RequestParam(value = "shareId", required = false) String shareId) {
        // Obtain the node entity. The method includes determining whether the node exists.
        NodeEntity node = iNodeService.getByNodeId(nodeId);
        ControlRole role;
        boolean nodeFavorite = false;
        if (!node.getIsTemplate()) {
            if (StrUtil.isNotBlank(shareId)) {
                // Open in node sharing to verify the sharing validity and whether the node has sharing.
                String shareNodeId = nodeShareSettingMapper.selectNodeIdByShareId(shareId);
                ExceptionUtil.isNotNull(shareNodeId, NodeException.SHARE_EXPIRE);
                if (!nodeId.equals(shareNodeId)) {
                    List<String> nodes = iNodeService.getPathParentNode(nodeId);
                    ExceptionUtil.isTrue(nodes.contains(shareNodeId), PermissionException.NODE_ACCESS_DENIED);
                }
                role = ControlRoleManager.parseNodeRole(Node.ANONYMOUS);
            }
            else {
                // The method includes determining whether the user is in this space.
                Long memberId = LoginContext.me().getUserSpaceDto(node.getSpaceId()).getMemberId();
                role = controlTemplate.fetchNodeRole(memberId, nodeId);
                // query whether the node is favorite
                nodeFavorite = SqlTool.retCount(nodeFavoriteMapper.countByMemberIdAndNodeId(memberId, nodeId)) > 0;
            }
        }
        else {
            role = ControlRoleManager.parseNodeRole(Node.TEMPLATE_VISITOR);
        }
        String description = nodeDescMapper.selectDescriptionByNodeId(nodeId);
        NodePermissionView permissions = role.permissionToBean(NodePermissionView.class);
        // query node creator basic information
        MemberDTO memberDto = memberMapper.selectMemberDtoByUserIdAndSpaceId(node.getCreatedBy(), node.getSpaceId());
        CreatedMemberInfoVo createdMemberInfo = null;
        if (null != memberDto) {
            createdMemberInfo = new CreatedMemberInfoVo();
            createdMemberInfo.setMemberName(memberDto.getMemberName());
            createdMemberInfo.setAvatar(memberDto.getAvatar());
        }
        NodeExtra extra = iNodeService.getNodeExtras(nodeId, node.getSpaceId(), node.getExtra());
        ShowcaseVo.Social social = null;
        if (StrUtil.isNotBlank(extra.getDingTalkCorpId())) {
            social = new Social(extra.getDingTalkDaStatus(), extra.getDingTalkSuiteKey(),
                    extra.getDingTalkCorpId(), extra.getSourceTemplateId(), extra.getShowTips());
        }
        ShowcaseVo vo = new ShowcaseVo(nodeId, node.getNodeName(), node.getType(), node.getIcon(), node.getCover(),
                description, role.getRoleTag(), permissions, nodeFavorite, createdMemberInfo, node.getUpdatedAt(),
                social, extra);
        return ResponseData.success(vo);
    }

    @GetResource(path = "/window", requiredPermission = false)
    @ApiOperation(value = "Node info window", notes = "Nodes that are not in the center of the template, make spatial judgments.")
    public ResponseData<NodeInfoWindowVo> showNodeInfoWindow(@RequestParam("nodeId") String nodeId) {
        // The method includes determining whether the user is in this space.
        Long userId = SessionContext.getUserId();
        // The method includes determining whether a node exists.
        String spaceId = iNodeService.getSpaceIdByNodeId(nodeId);
        // check permission
        Long memberId = LoginContext.me().getMemberId(userId, spaceId);
        controlTemplate.checkNodePermission(memberId, nodeId, NodePermission.READ_NODE,
                status -> ExceptionUtil.isTrue(status, PermissionException.NODE_OPERATION_DENIED));
        // build node info window
        return ResponseData.success(iNodeService.getNodeWindowInfo(nodeId));
    }

    @GetResource(path = "/parents", requiredPermission = false)
    @ApiOperation(value = "Get parent nodes", notes = "Gets a list of all parent nodes of the specified node " + ROLE_DESC)
    @ApiImplicitParam(name = "nodeId", value = "node id", required = true, dataTypeClass = String.class, paramType = "query", example = "nodRTGSy43DJ9")
    public ResponseData<List<NodePathVo>> getParentNodes(@RequestParam(name = "nodeId") String nodeId) {
        // The method includes determining whether a node exists.
        String spaceId = iNodeService.getSpaceIdByNodeId(nodeId);
        // check whether cross space
        LoginContext.me().getUserSpaceDto(spaceId);
        List<NodePathVo> nodePaths = iNodeService.getParentPathByNodeId(spaceId, nodeId);
        return ResponseData.success(nodePaths);
    }

    @GetResource(path = "/children", requiredPermission = false)
    @ApiOperation(value = "Get child nodes", notes = "Obtain the list of child nodes of the specified node. The nodes are classified into folders or datasheet by type " + ROLE_DESC)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "nodeId", value = "node id", required = true, dataTypeClass = String.class, paramType = "query", example = "nodRTGSy43DJ9"),
            @ApiImplicitParam(name = "nodeType", value = "node type 1:folder,2:datasheet", dataTypeClass = Integer.class, paramType = "query", example = "1")
    })
    public ResponseData<List<NodeInfoVo>> getNodeChildrenList(@RequestParam(name = "nodeId") String nodeId,
            @RequestParam(name = "nodeType", required = false) Integer nodeType) {
        // get the space ID, the method includes judging whether the node exists
        String spaceId = iNodeService.getSpaceIdByNodeId(nodeId);
        NodeType nodeTypeEnum = null;
        if (null != nodeType) {
            nodeTypeEnum = NodeType.toEnum(nodeType);
        }
        // The method includes determining whether the user is in this space.
        Long memberId = LoginContext.me().getUserSpaceDto(spaceId).getMemberId();
        List<NodeInfoVo> nodeInfos = iNodeService.getChildNodesByNodeId(spaceId, memberId, nodeId, nodeTypeEnum);
        return ResponseData.success(nodeInfos);
    }

    @GetResource(path = "/position/{nodeId}", requiredPermission = false)
    @ApiOperation(value = "Position node", notes = "node in must " + ROLE_DESC)
    @ApiImplicitParam(name = "nodeId", value = "node id", required = true, dataTypeClass = String.class, paramType = "path", example = "nodRTGSy43DJ9")
    public ResponseData<NodeInfoTreeVo> position(@PathVariable("nodeId") String nodeId) {
        // The method includes determining whether a node exists.
        String spaceId = iNodeService.getSpaceIdByNodeId(nodeId);
        // The method includes determining whether the user is in this space.
        Long memberId = LoginContext.me().getUserSpaceDto(spaceId).getMemberId();
        // check node permissions
        controlTemplate.checkNodePermission(memberId, nodeId, NodePermission.READ_NODE,
                status -> ExceptionUtil.isTrue(status, PermissionException.NODE_OPERATION_DENIED));
        NodeInfoTreeVo treeVo = iNodeService.position(spaceId, memberId, nodeId);
        return ResponseData.success(treeVo);
    }

    @Notification(templateId = NotificationTemplateId.NODE_CREATE)
    @PostResource(path = "/create", requiredPermission = false)
    @ApiOperation(value = "Create child node", notes = "create a new node under the node" + ROLE_DESC)
    @ApiImplicitParam(name = ParamsConstants.PLAYER_SOCKET_ID, value = "user socket id", dataTypeClass = String.class, paramType = "header", example = "QkKp9XJEl")
    public ResponseData<NodeInfoVo> create(@RequestBody @Valid NodeOpRo nodeOpRo) {
        Long userId = SessionContext.getUserId();
        // The method includes determining whether a node exists.
        String spaceId = iNodeService.getSpaceIdByNodeId(nodeOpRo.getParentId());
        SpaceHolder.set(spaceId);
        // The method includes determining whether the user is in this space.
        Long memberId = LoginContext.me().getMemberId(userId, spaceId);
        // Check whether the parent node has the specified operation permission
        iNodeService.checkEnableOperateNodeBySpaceFeature(memberId, spaceId, nodeOpRo.getParentId());
        ControlRole role = controlTemplate.fetchNodeRole(memberId, nodeOpRo.getParentId());
        ExceptionUtil.isTrue(role.hasPermission(NodePermission.CREATE_NODE), PermissionException.NODE_OPERATION_DENIED);
        // Check whether the source tables of form and mirror exist and whether they have the specified operation permissions.
        iNodeService.checkSourceDatasheet(spaceId, memberId, nodeOpRo.getType(), nodeOpRo.getExtra());
        String nodeId = iNodeService.createNode(userId, spaceId, nodeOpRo);
        // publish space audit events
        AuditSpaceArg arg = AuditSpaceArg.builder().action(AuditSpaceAction.CREATE_NODE).userId(userId).nodeId(nodeId).build();
        SpringContextHolder.getApplicationContext().publishEvent(new AuditSpaceEvent(this, arg));
        // The new node inherits parent node permissions by default
        return ResponseData.success(iNodeService.getNodeInfoByNodeId(spaceId, nodeId, role));
    }

    @Notification(templateId = NotificationTemplateId.NODE_UPDATE)
    @PostResource(path = "/update/{nodeId}", requiredPermission = false)
    @ApiOperation(value = "Edit node", notes = "node id must. name, icon is not required" + ROLE_DESC)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "nodeId", value = "node id", required = true, dataTypeClass = String.class, paramType = "path", example = "nodRTGSy43DJ9"),
            @ApiImplicitParam(name = ParamsConstants.PLAYER_SOCKET_ID, value = "user socket id", dataTypeClass = String.class, paramType = "header", example = "QkKp9XJEl")
    })
    public ResponseData<Void> update(@PathVariable("nodeId") String nodeId, @RequestBody @Valid NodeUpdateOpRo nodeOpRo) {
        ExceptionUtil.isTrue(StrUtil.isNotBlank(nodeOpRo.getNodeName()) || ObjectUtil.isNotNull(nodeOpRo.getIcon())
                || ObjectUtil.isNotNull(nodeOpRo.getCover()) || ObjectUtil.isNotNull(nodeOpRo.getShowRecordHistory()), ParameterException.NO_ARG);
        Long userId = SessionContext.getUserId();
        // The method includes determining whether a node exists.
        String spaceId = iNodeService.getSpaceIdByNodeId(nodeId);
        SpaceHolder.set(spaceId);
        // The method includes determining whether the user is in this space.
        Long memberId = LoginContext.me().getMemberId(userId, spaceId);
        // check whether the node has the specified operation permission
        controlTemplate.checkNodePermission(memberId, nodeId, NodePermission.MANAGE_NODE,
                status -> ExceptionUtil.isTrue(status, PermissionException.NODE_OPERATION_DENIED));
        iNodeService.edit(userId, nodeId, nodeOpRo);
        return ResponseData.success();
    }

    @Notification(templateId = NotificationTemplateId.NODE_UPDATE_DESC)
    @PostResource(path = "/updateDesc", requiredPermission = false)
    @ApiOperation(value = "Update node description")
    @ApiImplicitParam(name = ParamsConstants.PLAYER_SOCKET_ID, value = "user socket id", dataTypeClass = String.class, paramType = "header", example = "QkKp9XJEl")
    public ResponseData<Void> updateDesc(@RequestBody @Valid NodeDescOpRo opRo) {
        // The method includes determining whether a node exists.
        String spaceId = iNodeService.getSpaceIdByNodeId(opRo.getNodeId());
        SpaceHolder.set(spaceId);
        // The method includes determining whether the user is in this space.
        Long memberId = LoginContext.me().getUserSpaceDto(spaceId).getMemberId();
        // Check whether there is a specified operation permission under the node.
        controlTemplate.checkNodePermission(memberId, opRo.getNodeId(), NodePermission.EDIT_NODE_DESC,
                status -> ExceptionUtil.isTrue(status, PermissionException.NODE_OPERATION_DENIED));
        iNodeDescService.edit(opRo.getNodeId(), opRo.getDescription());
        // publish space audit events
        AuditSpaceArg arg = AuditSpaceArg.builder().action(AuditSpaceAction.UPDATE_NODE_DESC).userId(SessionContext.getUserId()).nodeId(opRo.getNodeId()).build();
        SpringContextHolder.getApplicationContext().publishEvent(new AuditSpaceEvent(this, arg));
        return ResponseData.success();
    }

    @Notification(templateId = NotificationTemplateId.NODE_MOVE)
    @PostResource(path = "/move")
    @ApiOperation(value = "Move node", notes = "Node ID and parent node ID are required, and pre Node Id is not required.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = ParamsConstants.SPACE_ID, value = "space id", required = true, dataTypeClass = String.class, paramType = "header", example = "spcyQkKp9XJEl"),
            @ApiImplicitParam(name = ParamsConstants.PLAYER_SOCKET_ID, value = "user socket id", dataTypeClass = String.class, paramType = "header", example = "QkKp9XJEl")
    })
    public ResponseData<List<NodeInfoVo>> move(@RequestBody @Valid NodeMoveOpRo nodeOpRo) {
        Long memberId = LoginContext.me().getMemberId();
        String spaceId = LoginContext.me().getSpaceId();
        SpaceHolder.set(spaceId);
        iNodeService.checkEnableOperateNodeBySpaceFeature(memberId, spaceId, nodeOpRo.getParentId());
        iNodeService.checkNodeIfExist(spaceId, nodeOpRo.getNodeId());
        iNodeService.checkNodeIfExist(spaceId, nodeOpRo.getParentId());
        if (StrUtil.isNotBlank(nodeOpRo.getPreNodeId())) {
            iNodeService.checkNodeIfExist(spaceId, nodeOpRo.getPreNodeId());
        }
        // manageable for this node
        controlTemplate.checkNodePermission(memberId, nodeOpRo.getNodeId(), NodePermission.MOVE_NODE,
                status -> ExceptionUtil.isTrue(status, PermissionException.NODE_OPERATION_DENIED));
        // check movement operation
        String parentId = iNodeService.getParentIdByNodeId(nodeOpRo.getNodeId());
        iNodeService.checkEnableOperateNodeBySpaceFeature(memberId, spaceId, parentId);
        if (parentId.equals(nodeOpRo.getParentId())) {
            // move under sibling
            controlTemplate.checkNodePermission(memberId, nodeOpRo.getParentId(), NodePermission.MANAGE_NODE,
                    status -> ExceptionUtil.isTrue(status, PermissionException.NODE_OPERATION_DENIED));
        }
        else {
            // manageable destination folder
            controlTemplate.checkNodePermission(memberId, nodeOpRo.getParentId(), NodePermission.CREATE_NODE,
                    status -> ExceptionUtil.isTrue(status, PermissionException.NODE_OPERATION_DENIED));
        }
        Long userId = SessionContext.getUserId();
        List<String> nodeIds = iNodeService.move(userId, nodeOpRo);
        return ResponseData.success(iNodeService.getNodeInfoByNodeIds(spaceId, memberId, nodeIds));
    }

    @Notification(templateId = NotificationTemplateId.NODE_DELETE)
    @PostResource(path = "/delete/{nodeId}", method = { RequestMethod.DELETE, RequestMethod.POST }, requiredPermission = false)
    @ApiOperation(value = "Delete node", notes = "You can pass in an ID array and delete multiple nodes.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "nodeId", value = "node id", required = true, dataTypeClass = String.class, paramType = "path", example = "nodRTGSy43DJ9"),
            @ApiImplicitParam(name = ParamsConstants.PLAYER_SOCKET_ID, value = "user socket id", dataTypeClass = String.class, paramType = "header", example = "QkKp9XJEl")
    })
    public ResponseData<Void> delete(@PathVariable("nodeId") String nodeId) {
        // The method includes determining whether a node exists.
        String spaceId = iNodeService.getSpaceIdByNodeId(nodeId);
        SpaceHolder.set(spaceId);
        // The method includes determining whether the user is in this space.
        Long memberId = LoginContext.me().getUserSpaceDto(spaceId).getMemberId();
        // root node cannot be deleted
        String rootNodeId = iNodeService.getRootNodeIdBySpaceId(spaceId);
        ExceptionUtil.isFalse(nodeId.equals(rootNodeId), PermissionException.NODE_OPERATION_DENIED);
        // Check whether there is a specified operation permission under the node.
        controlTemplate.checkNodePermission(memberId, nodeId, NodePermission.REMOVE_NODE,
                status -> ExceptionUtil.isTrue(status, PermissionException.NODE_OPERATION_DENIED));
        iNodeService.deleteById(spaceId, memberId, nodeId);
        // delete space capacity cache
        spaceCapacityCacheService.del(spaceId);
        return ResponseData.success();
    }

    @Notification(templateId = NotificationTemplateId.NODE_CREATE)
    @PostResource(path = "/copy", requiredPermission = false)
    @ApiOperation(value = "Copy node", notes = "node id is required, whether to copy data is not required.")
    @ApiImplicitParam(name = ParamsConstants.PLAYER_SOCKET_ID, value = "user socket id", dataTypeClass = String.class, paramType = "header", example = "QkKp9XJEl")
    public ResponseData<NodeInfoVo> copy(@RequestBody @Valid NodeCopyOpRo nodeOpRo) {
        Long userId = SessionContext.getUserId();
        // The method includes determining whether a node exists.
        String spaceId = iNodeService.getSpaceIdByNodeId(nodeOpRo.getNodeId());
        SpaceHolder.set(spaceId);
        // The method includes determining whether the user is in this space.
        Long memberId = LoginContext.me().getMemberId(userId, spaceId);
        // Verify the permissions of this node
        controlTemplate.checkNodePermission(memberId, nodeOpRo.getNodeId(), NodePermission.COPY_NODE,
                status -> ExceptionUtil.isTrue(status, PermissionException.NODE_OPERATION_DENIED));
        // Copy the datasheet requires the parent node's permission to create child nodes.
        String parentId = iNodeService.getParentIdByNodeId(nodeOpRo.getNodeId());
        ControlRole role = controlTemplate.fetchNodeRole(memberId, parentId);
        ExceptionUtil.isTrue(role.hasPermission(NodePermission.CREATE_NODE), PermissionException.NODE_OPERATION_DENIED);
        // replication node
        NodeCopyEffectDTO copyEffect = iNodeService.copy(userId, nodeOpRo);
        iNodeService.nodeCopyChangeset(copyEffect);
        // publish space audit events
        AuditSpaceArg arg = AuditSpaceArg.builder().action(AuditSpaceAction.COPY_NODE).userId(userId).nodeId(copyEffect.getCopyNodeId())
                .info(JSONUtil.createObj().set(AuditConstants.SOURCE_NODE_ID, nodeOpRo.getNodeId()).set(AuditConstants.RECORD_COPYABLE, nodeOpRo.getData())).build();
        SpringContextHolder.getApplicationContext().publishEvent(new AuditSpaceEvent(this, arg));
        // The new node inherits parent node permissions by default
        return ResponseData.success(iNodeService.getNodeInfoByNodeId(spaceId, copyEffect.getCopyNodeId(), role));
    }

    @GetResource(path = "/exportBundle", requiredPermission = false)
    @ApiOperation(value = "Export VikaBundle")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "nodeId", value = "node id", required = true, dataTypeClass = String.class, paramType = "query", example = "fod8mXUeiXyVo"),
            @ApiImplicitParam(name = "saveData", value = "whether to retain data", dataTypeClass = Boolean.class, paramType = "query", example = "true"),
            @ApiImplicitParam(name = "password", value = "encrypted password", dataTypeClass = String.class, paramType = "query", example = "qwer1234")
    })
    public void exportBundle(@RequestParam("nodeId") String nodeId,
            @RequestParam(value = "saveData", required = false, defaultValue = "true") Boolean saveData,
            @RequestParam(value = "password", required = false) String password) {
        Long userId = SessionContext.getUserId();
        // The method includes determining whether a node exists.
        String spaceId = iNodeService.getSpaceIdByNodeId(nodeId);
        SpaceHolder.set(spaceId);
        // The method includes determining whether the user is in this space.
        Long memberId = LoginContext.me().getMemberId(userId, spaceId);
        // check node permissions
        controlTemplate.checkNodePermission(memberId, nodeId, NodePermission.MANAGE_NODE,
                status -> ExceptionUtil.isTrue(status, PermissionException.NODE_OPERATION_DENIED));
        // Verify the permissions of all child and descendant nodes
        iNodeService.checkSubNodePermission(memberId, nodeId, ControlRoleManager.parseNodeRole(Node.MANAGER));
        vikaBundleService.generate(nodeId, saveData, password);
    }

    @PostResource(path = "/analyzeBundle", requiredLogin = false)
    @ApiOperation(value = "Analyze VikaBundle", notes = "The front node is saved in the first place of the parent node when it is not under the parent node. Save in the first place of the first level directory when it is not transmitted.", produces = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseData<Void> analyzeBundle(@Valid VikaBundleOpRo opRo) {
        String parentId = opRo.getParentId();
        Long userId = SessionContext.getUserId();
        // The method includes determining whether a node exists.
        String spaceId = iNodeService.getSpaceIdByNodeId(parentId);
        SpaceHolder.set(spaceId);
        // The method includes determining whether the user is in this space.
        Long memberId = LoginContext.me().getMemberId(userId, spaceId);
        // verify parent node permissions
        if (StrUtil.isNotBlank(parentId)) {
            controlTemplate.checkNodePermission(memberId, parentId, NodePermission.CREATE_NODE,
                    status -> ExceptionUtil.isTrue(status, PermissionException.NODE_OPERATION_DENIED));
        }
        if (StrUtil.isBlank(parentId) && StrUtil.isBlank(opRo.getPreNodeId())) {
            parentId = iNodeService.getRootNodeIdBySpaceId(spaceId);
        }
        vikaBundleService.analyze(opRo.getFile(), opRo.getPassword(), parentId, opRo.getPreNodeId(), userId);
        return ResponseData.success();
    }

    @Notification(templateId = NotificationTemplateId.NODE_CREATE)
    @PostResource(path = "/import", requiredPermission = false)
    @ApiOperation(value = "Import excel", notes = "all parameters must be")
    public ResponseData<NodeInfoVo> importExcel(@Valid ImportExcelOpRo data) throws IOException {
        ExceptionUtil.isTrue(data.getFile().getSize() <= limitProperties.getMaxFileSize(), ActionException.FILE_EXCEED_LIMIT);
        Long userId = SessionContext.getUserId();
        // The method includes determining whether a node exists.
        String spaceId = iNodeService.getSpaceIdByNodeId(data.getParentId());
        SpaceHolder.set(spaceId);
        // The method includes determining whether the user is in this space.
        Long memberId = LoginContext.me().getMemberId(userId, spaceId);
        // Check whether there is a specified operation permission under the node.
        ControlRole role = controlTemplate.fetchNodeRole(memberId, data.getParentId());
        ExceptionUtil.isTrue(role.hasPermission(NodePermission.CREATE_NODE), PermissionException.NODE_OPERATION_DENIED);
        // String nodeId = iNodeService.importExcel(userId, spaceId, data);
        // The new node inherits parent node permissions by default
        String uuid = userMapper.selectUuidById(userId);
        // fileName
        String mainName = cn.hutool.core.io.FileUtil.mainName(data.getFile().getOriginalFilename());
        if (StrUtil.isBlank(mainName)) {
            throw new BusinessException("file name is empty");
        }
        mainName = iNodeService.duplicateNameModify(data.getParentId(), NodeType.DATASHEET.getNodeType(), mainName, null);
        // file type suffix
        String fileSuffix = cn.hutool.core.io.FileUtil.extName(data.getFile().getOriginalFilename());
        if (StrUtil.isBlank(fileSuffix)) {
            throw new BusinessException("file name suffix must not be empty");
        }
        String createNodeId;
        if (FileSuffixConstants.CSV.equals(fileSuffix)) {
            // identification file code
            String encoding = FileTool.identifyCoding(data.getFile().getInputStream());
            // Regenerate the byte stream according to the identification file encoding
            InputStream targetInputStream =
                    new ByteArrayInputStream(IOUtils.toString(data.getFile().getInputStream(), encoding).getBytes());
            createNodeId = iNodeService.parseCsv(userId, uuid, spaceId, memberId, data.getParentId(), mainName, targetInputStream);
        }
        else {
            createNodeId = iNodeService.parseExcel(userId, uuid, spaceId, memberId, data.getParentId(), mainName, fileSuffix, data.getFile().getInputStream());
        }
        // publish space audit events
        AuditSpaceArg arg = AuditSpaceArg.builder().action(AuditSpaceAction.IMPORT_NODE).userId(userId).nodeId(createNodeId).build();
        SpringContextHolder.getApplicationContext().publishEvent(new AuditSpaceEvent(this, arg));
        return ResponseData.success(iNodeService.getNodeInfoByNodeId(spaceId, createNodeId, role));
    }

    @PostResource(name = "record active nodes", path = "/active", requiredPermission = false)
    @ApiOperation(value = "Record active node", notes = "node id and view id are not required（do not pass means all closed）")
    @ApiImplicitParam(name = ParamsConstants.SPACE_ID, value = "space id", required = true, dataTypeClass = String.class, paramType = "header", example = "spcBrtP3ulTXR")
    public ResponseData<Void> activeSheets(@RequestBody @Valid ActiveSheetsOpRo opRo) {
        Long userId = SessionContext.getUserId();
        String spaceId;
        if (opRo.getNodeId() == null) {
            spaceId = LoginContext.me().getSpaceId();
            userSpaceOpenedSheetService.refresh(userId, spaceId, null);
        }
        else {
            // The method includes determining whether a node exists.
            spaceId = iNodeService.getSpaceIdByNodeId(opRo.getNodeId());
            OpenedSheet openedSheet = OpenedSheet.builder().nodeId(opRo.getNodeId()).viewId(opRo.getViewId()).position(opRo.getPosition()).build();
            userSpaceOpenedSheetService.refresh(userId, spaceId, openedSheet);
        }
        // check if space is spanned
        LoginContext.me().checkAcrossSpace(userId, spaceId);
        // Cache the space activated by the user's last operation
        userActiveSpaceService.save(userId, spaceId);
        return ResponseData.success();
    }

    @PostResource(name = "Remind notification", path = "/remind", requiredLogin = false)
    @ApiOperation(value = "Remind notification")
    public ResponseData<Void> remind(@RequestBody @Valid RemindMemberRo ro) {
        Long userId = SessionContext.getUserIdWithoutException();
        // Obtain the space ID. The method includes determining whether the node exists.
        String spaceId = iNodeService.getSpaceIdByNodeId(ro.getNodeId());
        if (StrUtil.isBlank(ro.getLinkId())) {
            // In the space station, check whether it crosses space
            LoginContext.me().getUserSpaceDto(spaceId);
        }
        else {
            // node sharing
            String shareSpaceId = nodeShareSettingMapper.selectSpaceIdByShareId(ro.getLinkId());
            ExceptionUtil.isNotNull(shareSpaceId, NodeException.SHARE_EXPIRE);
            ExceptionUtil.isTrue(shareSpaceId.equals(spaceId), SpaceException.NOT_IN_SPACE);
        }
        datasheetService.remindMemberRecOp(userId, spaceId, ro);
        return ResponseData.success();
    }

    @PostResource(path = "/remind/units/noPermission", requiredPermission = false)
    @ApiOperation(value = "Gets no permission member before remind")
    public ResponseData<List<MemberBriefInfoVo>> postRemindUnitsNoPermission(@RequestBody @Validated RemindUnitsNoPermissionRo request) {

        // Get a list of all members under the organizational unit
        List<Long> allMemberIds = unitService.getMembersIdByUnitIds(request.getUnitIds());
        String nodeId = request.getNodeId();
        // list of member ids without permissions
        List<Long> noPermissionMemberIds = allMemberIds.stream()
                .filter(memberId -> !controlTemplate.hasNodePermission(memberId, nodeId, NodePermission.READ_NODE))
                .collect(Collectors.toList());

        return ResponseData.success(memberService.getMemberBriefInfo(noPermissionMemberIds));

    }

    @GetResource(path = "/checkRelNode", requiredPermission = false)
    @ApiOperation(value = "check for associated nodes", notes = "permission of the associated node is not required. Scenario: Check whether the view associated mirror before deleting the table.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "nodeId", value = "node id", required = true, dataTypeClass = String.class, paramType = "query", example = "dstU8Agt2Je9J7AKsv"),
            @ApiImplicitParam(name = "viewId", value = "view id（do not specify full return）", dataTypeClass = String.class, paramType = "query", example = "viwF1CqEW2GxY"),
            @ApiImplicitParam(name = "type", value = "node type（do not specify full return，form:3/mirror:5）", dataTypeClass = Integer.class, paramType = "query", example = "5")
    })
    public ResponseData<List<NodeInfo>> checkRelNode(@RequestParam("nodeId") String nodeId,
            @RequestParam(value = "viewId", required = false) String viewId,
            @RequestParam(value = "type", required = false) Integer type) {
        return ResponseData.success(iNodeRelService.getRelationNodeInfoByNodeId(nodeId, viewId, null, type));
    }

    @GetResource(path = "/getRelNode", requiredPermission = false)
    @ApiOperation(value = "Get associated node", notes = "This interface requires readable or above permissions of the associated node.Scenario: Open the display columns of form and mirror in the datasheet.")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "nodeId", value = "node id", required = true, dataTypeClass = String.class, paramType = "query", example = "dstU8Agt2Je9J7AKsv"),
            @ApiImplicitParam(name = "viewId", value = "view id（do not specify full return）", dataTypeClass = String.class, paramType = "query", example = "viwF1CqEW2GxY"),
            @ApiImplicitParam(name = "type", value = "node type（do not specify full return，form:3/mirror:5）", dataTypeClass = Integer.class, paramType = "query", example = "5")
    })
    public ResponseData<List<NodeInfo>> getNodeRel(@RequestParam("nodeId") String nodeId,
            @RequestParam(value = "viewId", required = false) String viewId,
            @RequestParam(value = "type", required = false) Integer type) {
        Long userId = SessionContext.getUserId();
        // The method includes determining whether a node exists.
        String spaceId = iNodeService.getSpaceIdByNodeId(nodeId);
        // The method includes determining whether the user is in this space.
        Long memberId = LoginContext.me().getMemberId(userId, spaceId);
        // check node permissions
        controlTemplate.checkNodePermission(memberId, nodeId, NodePermission.READ_NODE,
                status -> ExceptionUtil.isTrue(status, PermissionException.NODE_OPERATION_DENIED));
        return ResponseData.success(iNodeRelService.getRelationNodeInfoByNodeId(nodeId, viewId, memberId, type));
    }

    @GetResource(path = "/recentList", requiredPermission = false)
    @ApiOperation(value = "member recent open node list", notes = "member recent open node list")
    @ApiImplicitParam(name = ParamsConstants.SPACE_ID, value = "space id", required = true, dataTypeClass = String.class, paramType = "header", example = "spcyQkKp9XJEl")
    public ResponseData<List<NodeSearchResult>> recentList() {
        String spaceId = LoginContext.me().getSpaceId();
        Long memberId = LoginContext.me().getMemberId();
        List<NodeSearchResult> nodeInfos = iNodeService.recentList(spaceId, memberId);
        return ResponseData.success(nodeInfos);
    }

}
