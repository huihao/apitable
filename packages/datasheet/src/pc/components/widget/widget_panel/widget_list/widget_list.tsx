import { ContextMenu, useThemeColors } from '@vikadata/components';
import {
  CollaCommandName,
  ConfigConstant,
  IWidget,
  ResourceType,
  Selectors,
  Strings,
  SystemConfig,
  t,
  WidgetPackageStatus,
  WidgetReleaseType,
} from '@vikadata/core';
import { CodeFilled, DashboardOutlined, DeleteOutlined, EditOutlined, InformationSmallOutlined, SettingOutlined } from '@vikadata/icons';
import { useLocalStorageState } from 'ahooks';
import classNames from 'classnames';
import { keyBy } from 'lodash';
import { TriggerCommands } from 'pc/common/apphook/trigger_commands';
import { EmitterEventName } from 'pc/common/simple_emitter';
import { Modal } from 'pc/components/common';
import { ScreenSize } from 'pc/components/common/component_display';
import { simpleEmitter as panelSimpleEmitter } from 'pc/components/common/vika_split_panel';
import { expandWidgetRoute } from 'pc/components/widget/expand_widget';
import { useResponsive } from 'pc/hooks';
import { resourceService } from 'pc/resource_service';
import { flatContextData } from 'pc/utils';
import { useEffect, useState } from 'react';
import { Responsive, WidthProvider } from 'react-grid-layout';
import { useSelector } from 'react-redux';
import { expandPublishHelp } from '../../widget_center/widget_create_modal';
import { openSendToDashboard } from '../send_to_dashboard';
import { simpleEmitter, WidgetItem } from '../widget_item';
import styles from './style.module.less';

const ResponsiveGridLayout = WidthProvider(Responsive);

export const WIDGET_MENU = 'WIDGET_MENU';

export const WidgetList = () => {
  const colors = useThemeColors();
  const { datasheetId, widgetId, mirrorId } = useSelector(state => state.pageParams);
  const resourceId = mirrorId || datasheetId;
  const resourceType = mirrorId ? ResourceType.Mirror : ResourceType.Datasheet;
  const activeWidgetPanel = useSelector(state => {
    return Selectors.getResourceActiveWidgetPanel(state, resourceId!, resourceType);
  })!;
  const widgetList = activeWidgetPanel.widgets;
  const { editable, manageable } = useSelector(state => {
    return Selectors.getResourcePermission(state, datasheetId!, ResourceType.Datasheet);
  });
  const linkId = useSelector(Selectors.getLinkId);
  const hadWidgetExpanding = Boolean(widgetId);
  const [devWidgetId, setDevWidgetId] = useLocalStorageState<string>('devWidgetId');
  const [activeMenuWidget, setActiveMenuWidget] = useState<IWidget>();
  const widgetMap = useSelector(state => state.widgetMap);
  const isShowWidget = useSelector(state => Selectors.labsFeatureOpen(state, SystemConfig.test_function.widget_center.feature_key));
  const readonly = !editable;
  // 是否在缩放中
  const [dragging, setDragging] = useState<boolean>(false);

  const { screenIsAtMost } = useResponsive();
  const isMobile = screenIsAtMost(ScreenSize.md);

  useEffect(() => {
    simpleEmitter.bind(EmitterEventName.ToggleWidgetDevMode, widgetId => {
      setDevWidgetId(widgetId);
    });
    return () => simpleEmitter.unbind(EmitterEventName.ToggleWidgetDevMode);
  }, [setDevWidgetId]);

  useEffect(() => {
    panelSimpleEmitter.bind(EmitterEventName.PanelDragging, panelDragging => {
      setDragging(panelDragging);
    });
    return () => panelSimpleEmitter.unbind(EmitterEventName.PanelDragging);
  }, [setDragging]);

  const recordWidgetHeight = (widgetId: string, widgetHeight: number) => {
    resourceService.instance!.commandManager.execute({
      cmd: CollaCommandName.ChangeWidgetInPanelHeight,
      widgetHeight: widgetHeight,
      datasheetId: datasheetId!,
      panelId: activeWidgetPanel.id,
      widgetId,
    });
  };

  const onResizeStop = (layout, oldItem, newItem) => {
    setDragging(false);
    recordWidgetHeight(newItem.i, newItem.h);
  };

  const deleteWidget = ({ props }: { props?: any }) => {
    const { widgetId, deleteCb } = props;
    Modal.confirm({
      title: t(Strings.delete_widget_title),
      content: t(Strings.delete_widget_content),
      onOk: () => {
        resourceService.instance!.commandManager.execute({
          cmd: CollaCommandName.DeleteWidget,
          resourceId: resourceId!,
          resourceType: resourceType,
          widgetId,
        });
        deleteCb?.();
      },
      type: 'danger',
    });
  };

  const onDragStop = (layout, oldItem, newItem) => {
    setDragging(false);
    const layoutMap = keyBy(layout, 'i');
    const _layout = widgetList.map(item => {
      const widgetPosition = layoutMap[item.id];
      return {
        id: widgetPosition.i,
        y: widgetPosition.y,
        height: widgetPosition.h,
      };
    });
    resourceService.instance!.commandManager.execute({
      cmd: CollaCommandName.MoveWidget,
      resourceId: resourceId!,
      resourceType,
      layout: _layout,
      panelId: activeWidgetPanel.id,
    });
  };

  const renameWidget = ({ props }: { props?: any }) => {
    const { renameCb } = props;
    renameCb && renameCb();
  };
  const isWidgetBan = () => WidgetPackageStatus.Ban === activeMenuWidget?.status;
  const isWidgetPublished = () => WidgetPackageStatus.Published === activeMenuWidget?.status;
  const isWidgetDev = () => activeMenuWidget?.id === devWidgetId;
  const isWidgetGlobal = () => Boolean(activeMenuWidget?.id && widgetMap[activeMenuWidget.id]?.widget.releaseType === WidgetReleaseType.Global);
  const menuData = [
    [
      {
        icon: <SettingOutlined color={colors.thirdLevelText} />,
        text: t(Strings.widget_operate_setting),
        hidden: readonly || hadWidgetExpanding,
        onClick: ({ props }: { props?: any }) => {
          const { widgetId } = props;
          props.toggleSetting();
          expandWidgetRoute(widgetId);
        },
      },
      {
        icon: <CodeFilled color={colors.thirdLevelText} />,
        text: t(Strings.widget_operate_enter_dev),
        hidden: readonly || !isShowWidget || isWidgetBan() || isWidgetDev() || isWidgetGlobal(),
        onClick: ({ props }: { props?: any }) => {
          props?.toggleWidgetDevMode(devWidgetId, setDevWidgetId);
        },
      },
      {
        icon: <CodeFilled color={colors.thirdLevelText} />,
        text: t(Strings.widget_operate_exit_dev),
        hidden: readonly || !isShowWidget || isWidgetBan() || !isWidgetDev(),
        onClick: ({ props }: { props?: any }) => {
          props?.toggleWidgetDevMode(devWidgetId, setDevWidgetId);
          TriggerCommands.open_guide_wizard(ConfigConstant.WizardIdConstant.RELEASE_WIDGET_GUIDE);
        },
      },
      {
        icon: <EditOutlined color={colors.thirdLevelText} />,
        text: t(Strings.widget_operate_rename),
        hidden: readonly || isWidgetBan(),
        onClick: renameWidget,
      },
      {
        icon: <InformationSmallOutlined color={colors.thirdLevelText} />,
        text: t(Strings.widget_operate_publish_help),
        hidden: readonly || !isShowWidget || !isWidgetDev(),
        onClick: () => {
          expandPublishHelp();
        },
      },
      {
        icon: <DashboardOutlined color={colors.thirdLevelText} />,
        text: t(Strings.widget_operate_send_dashboard),
        onClick: ({ props }: { props?: any }) => {
          const { widgetId } = props;
          openSendToDashboard(widgetId);
        },
        hidden: Boolean(linkId) || !isWidgetPublished() || isWidgetDev(),
      },
    ],
    [
      {
        icon: <DeleteOutlined color={colors.thirdLevelText} />,
        text: t(Strings.widget_operate_delete),
        hidden: !manageable,
        onClick: deleteWidget,
      },
    ],
  ];

  return (
    <div className={styles.widgetList}>
      <ResponsiveGridLayout
        cols={{
          lg: 1,
          md: 1,
          sm: 1,
          xs: 1,
          xxs: 1,
        }}
        layouts={{
          lg: widgetList!.map(item => {
            return { w: 1, h: item.height, x: 0, y: item.y ?? 0, minH: 6.2, i: item.id };
          }),
        }}
        preventCollision={false}
        rowHeight={16}
        useCSSTransforms
        onDrag={() => setDragging(true)}
        onResizeStart={() => setDragging(true)}
        onResizeStop={onResizeStop}
        onDragStop={onDragStop}
        isBounded={false}
        draggableHandle={'.dragHandle'}
        draggableCancel={'.dragHandleDisabled'}
        isDroppable={manageable}
        isResizable={manageable}
        margin={[0, 24]}
      >
        {widgetList.map((item, index) => {
          const widgetMapItem = widgetMap?.[item.id]?.widget;
          const isDevMode = widgetMapItem?.status !== WidgetPackageStatus.Ban && devWidgetId === item.id;
          return (
            <div
              key={item.id}
              data-widget-id={item.id}
              data-guide-id="WIDGET_ITEM_WRAPPER"
              tabIndex={-1}
              className={classNames(styles.widgetItemWrap, widgetId === item.id && styles.isFullscreen)}
            >
              <WidgetItem
                index={index}
                widgetId={item.id}
                widgetPanelId={activeWidgetPanel.id}
                readonly={readonly}
                config={{ isDevMode, hideMoreOperate: isMobile }}
                setDevWidgetId={setDevWidgetId}
                dragging={dragging}
                setDragging={setDragging}
                isMobile={isMobile}
              />
            </div>
          );
        })}
      </ResponsiveGridLayout>
      <ContextMenu overlay={flatContextData(menuData, true)} onShown={({ props }) => setActiveMenuWidget(props?.widget)} menuId={WIDGET_MENU} />
    </div>
  );
};
