import { HttpModule } from '@nestjs/axios';
import { Global, Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { AutomationActionTypeRepository } from 'automation/repositories/automation.action.type.repository';
import { AutomationRobotRepository } from 'automation/repositories/automation.robot.repository';
import { AutomationRunHistoryRepository } from 'automation/repositories/automation.run.history.repository';
import { AutomationServiceRepository } from 'automation/repositories/automation.service.repository';
import { AutomationTriggerRepository } from 'automation/repositories/automation.trigger.repository';
import { AutomationTriggerTypeRepository } from 'automation/repositories/automation.trigger.type.repository';
import { AutomationService } from 'automation/services/automation.service';
import { AssetRepository } from 'database/repositories/asset.repository';
import { DatasheetChangesetRepository } from 'database/repositories/datasheet.changeset.repository';
import { DatasheetChangesetSourceRepository } from 'database/repositories/datasheet.changeset.source.repository';
import { DatasheetMetaRepository } from 'database/repositories/datasheet.meta.repository';
import { DatasheetRecordAlarmRepository } from 'database/repositories/datasheet.record.alarm.repository';
import { DatasheetRecordRepository } from 'database/repositories/datasheet.record.repository';
import { DatasheetRecordSourceRepository } from 'database/repositories/datasheet.record.source.repository';
import { DatasheetRecordSubscriptionRepository } from 'database/repositories/datasheet.record.subscription.repository';
import { DatasheetRepository } from 'database/repositories/datasheet.repository';
import { DatasheetWidgetRepository } from 'database/repositories/datasheet.widget.repository';
import { NodeDescRepository } from 'database/repositories/node.desc.repository';
import { NodeRelRepository } from 'database/repositories/node.rel.repository';
import { NodeRepository } from 'database/repositories/node.repository';
import { NodeShareSettingRepository } from 'database/repositories/node.share.setting.repository';
import { RecordCommentRepository } from 'database/repositories/record.comment.repository';
import { ResourceMetaRepository } from 'database/repositories/resource.meta.repository';
import { UnitMemberRepository } from 'database/repositories/unit.member.repository';
import { UnitRepository } from 'database/repositories/unit.repository';
import { UnitTagRepository } from 'database/repositories/unit.tag.repository';
import { UnitTeamRepository } from 'database/repositories/unit.team.repository';
import { UserRepository } from 'database/repositories/user.repository';
import { WidgetRepository } from 'database/repositories/widget.repository';
import { AttachmentService } from 'database/services/attachment/attachment.service';
import { DashboardService } from 'database/services/dashboard/dashboard.service';
import { ComputeFieldReferenceManager } from 'database/services/datasheet/compute.field.reference.manager';
import { DatasheetChangesetService } from 'database/services/datasheet/datasheet.changeset.service';
import { DatasheetChangesetSourceService } from 'database/services/datasheet/datasheet.changeset.source.service';
import { DatasheetFieldHandler } from 'database/services/datasheet/datasheet.field.handler';
import { DatasheetMetaService } from 'database/services/datasheet/datasheet.meta.service';
import { DatasheetRecordAlarmService } from 'database/services/datasheet/datasheet.record.alarm.service';
import { DatasheetRecordService } from 'database/services/datasheet/datasheet.record.service';
import { DatasheetRecordSourceService } from 'database/services/datasheet/datasheet.record.source.service';
import { DatasheetRecordSubscriptionService } from 'database/services/datasheet/datasheet.record.subscription.service';
import { DatasheetService } from 'database/services/datasheet/datasheet.service';
import { RecordCommentService } from 'database/services/datasheet/record.comment.service';
import { EventService } from 'database/services/event/event.service';
import { MirrorService } from 'database/services/mirror/mirror.service';
import { NodeDescriptionService } from 'database/services/node/node.description.service';
import { NodePermissionService } from 'database/services/node/node.permission.service';
import { NodeService } from 'database/services/node/node.service';
import { NodeShareSettingService } from 'database/services/node/node.share.setting.service';
import { DashboardOtService } from 'database/services/ot/dashboard.ot.service';
import { DatasheetOtService } from 'database/services/ot/datasheet.ot.service';
import { FormOtService } from 'database/services/ot/form.ot.service';
import { MirrorOtService } from 'database/services/ot/mirror.ot.service';
import { OtService } from 'database/services/ot/ot.service';
import { ResourceChangeHandler } from 'database/services/ot/resource.change.handler';
import { WidgetOtService } from 'database/services/ot/widget.ot.service';
import { UnitMemberService } from 'database/services/unit/unit.member.service';
import { UnitService } from 'database/services/unit/unit.service';
import { UnitTagService } from 'database/services/unit/unit.tag.service';
import { UnitTeamService } from 'database/services/unit/unit.team.service';
import { UserService } from 'database/services/user/user.service';
import { WidgetService } from 'database/services/widget/widget.service';
import { QueueWorkerModule } from 'enterprise/shared/queue.worker.module';
import { FusionApiFilter } from 'fusion/filter/fusion.api.filter';
import { ApiUsageRepository } from 'fusion/repositories/api.usage.repository';
import { FusionApiRecordService } from 'fusion/services/fusion.api.record.service';
import { FusionApiService } from 'fusion/services/fusion.api.service';
import { FusionApiTransformer } from 'fusion/transformer/fusion.api.transformer';
import { GrpcClientModule } from 'proto/client/grpc.client.module';
import { GlobalModule } from './global.module';
import { LoggerModule } from './logger/winston.module';
import { HttpConfigService } from './services/config/http.config.service';
import { LoggerConfigService } from './services/config/logger.config.service';
import { ZipkinConfigService } from './services/config/zipkin.config.service';
import { JavaModule } from './services/java/java.module';
import { ZipkinModule } from './services/zipkin/zipkin.module';

@Module({
  imports: [
  GlobalModule,
  TypeOrmModule.forFeature([AssetRepository]),
  JavaModule,
  HttpModule.registerAsync({
    useClass: HttpConfigService,
    }),

// Zipkin 配置
  ZipkinModule.forRootAsync({
    useClass: ZipkinConfigService,
    }),
// 日志配置
  LoggerModule.forRootAsync({
    useClass: LoggerConfigService,
    }),
  TypeOrmModule.forFeature([UserRepository]),
  TypeOrmModule.forFeature([
    WidgetRepository,
    ]),
  TypeOrmModule.forFeature([ApiUsageRepository]),
  JavaModule,
  // ResourceServiceModule,
  TypeOrmModule.forFeature([UserRepository]),
  TypeOrmModule.forFeature([DatasheetRecordRepository]),
  TypeOrmModule.forFeature([ResourceMetaRepository, DatasheetWidgetRepository]),
  // ResourceServiceModule,
  // HttpModule.registerAsync({
  //   useClass: HttpConfigService,
  //   }),
  GrpcClientModule,
  TypeOrmModule.forFeature([AutomationTriggerRepository, AutomationTriggerTypeRepository]),
  
  TypeOrmModule.forFeature([
    NodeRepository,
    AutomationTriggerRepository,
    AutomationRobotRepository,
    AutomationRunHistoryRepository,
    AutomationServiceRepository,
    AutomationTriggerTypeRepository,
    AutomationActionTypeRepository
    ]),
  TypeOrmModule.forFeature([ResourceMetaRepository]),
  TypeOrmModule.forFeature([UnitRepository, UnitMemberRepository, UnitTagRepository, UnitTeamRepository, UserRepository]),
  TypeOrmModule.forFeature([
    NodeRepository,
    NodeRelRepository,
    NodeDescRepository,
    NodeShareSettingRepository,
    DatasheetRepository,
    ResourceMetaRepository,
    ]),
  // HttpModule.registerAsync({
  //   useClass: HttpConfigService,
  //   }),
  TypeOrmModule.forFeature([UnitRepository, UnitMemberRepository, UnitTagRepository, UnitTeamRepository, UserRepository]),
 
  TypeOrmModule.forFeature([
    WidgetRepository,
    ]),
  TypeOrmModule.forFeature([
    DatasheetRecordRepository,
    DatasheetRecordSourceRepository,
    DatasheetRecordSubscriptionRepository,
    DatasheetRepository,
    DatasheetMetaRepository,
    RecordCommentRepository,
    DatasheetChangesetRepository,
    DatasheetChangesetSourceRepository,
    DatasheetRecordAlarmRepository,
    ]),
  // UserServiceModule,
  TypeOrmModule.forFeature([UserRepository]),
  JavaModule,
  QueueWorkerModule,

  TypeOrmModule.forFeature([UnitRepository, UnitMemberRepository, UnitTagRepository, UnitTeamRepository, UserRepository]),
  // UserServiceModule,
  // HttpModule.registerAsync({
  //   useClass: HttpConfigService,
  //   }),
  TypeOrmModule.forFeature([
    NodeRepository,
    NodeRelRepository,
    NodeDescRepository,
    NodeShareSettingRepository,
    DatasheetRepository,
    ResourceMetaRepository,
    ]),
  // UserServiceModule,
  // HttpModule.registerAsync({
  //   useClass: HttpConfigService,
  //   }),
  TypeOrmModule.forFeature([UnitRepository, UnitMemberRepository, UnitTagRepository, UnitTeamRepository, UserRepository]),
  // UserServiceModule, 
  ],

  controllers: [],
  providers: [AttachmentService,UserService,
  FusionApiRecordService, FusionApiService, FusionApiFilter, FusionApiTransformer,
// RestService,
  OtService,
  DatasheetOtService,
  DashboardOtService,
  MirrorOtService,
  FormOtService,
  WidgetOtService,
  ResourceChangeHandler,
  MirrorService,
  EventService,
  AutomationService,
  DashboardService,
  UnitService, UnitMemberService, UnitTagService, UnitTeamService,
  WidgetService,
  NodeService, NodePermissionService, NodeShareSettingService, NodeDescriptionService, 
  UserService ,
  UnitService, UnitMemberService, UnitTagService, UnitTeamService,
  DatasheetService,
  DatasheetMetaService,
  DatasheetRecordService,
  RecordCommentService,
  DatasheetRecordSourceService,
  DatasheetRecordSubscriptionService,
  DatasheetFieldHandler,
  ComputeFieldReferenceManager,
  DatasheetChangesetService,
  DatasheetChangesetSourceService,
  DatasheetRecordAlarmService,
  UserService ,
  WidgetService,
  UnitService, UnitMemberService, UnitTagService, UnitTeamService,
  NodeService, NodePermissionService, NodeShareSettingService, NodeDescriptionService, 
  
  UnitService, UnitMemberService, UnitTagService, UnitTeamService
  ],
  exports: [AttachmentService, UserService,FusionApiService, FusionApiFilter, FusionApiTransformer,
  DatasheetService,
  DatasheetMetaService,
  DatasheetRecordService,
  RecordCommentService,
  DatasheetRecordSourceService,
  DatasheetRecordSubscriptionService,
  DatasheetFieldHandler,
  ComputeFieldReferenceManager,
  DatasheetChangesetService,
  DatasheetChangesetSourceService,
  DatasheetRecordAlarmService,
  ],
  })
export class SharedModule {}
