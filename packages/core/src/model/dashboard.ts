import { IDashboardLayout, IDashboardSnapshot } from '../store';
import { IJOTAction } from 'engine';
import { OTActionName } from '../engine/ot';

export class DashboardAction {
  // 安装小组件
  static addWidget2Action(
    snapshot: IDashboardSnapshot,
    options: {
      layout: IDashboardLayout[];
    },
  ): IJOTAction[] | null {
    const widgetInstallations = snapshot.widgetInstallations;
    const widgetCount = widgetInstallations.layout?.length;
    const { layout } = options;
    if (!widgetCount) {
      // 第一次加入
      return [
        {
          n: OTActionName.ObjectInsert,
          p: ['widgetInstallations', 'layout'],
          oi: layout,
        },
      ];
    }

    const action: IJOTAction[] = [];

    layout.forEach((widget, index) => {
      action.push({
        n: OTActionName.ListInsert,
        p: ['widgetInstallations', 'layout', widgetCount + index],
        li: widget,
      });
    });

    return action;
  }

  // 删除小组件
  static deleteWidget2Action(snapshot: IDashboardSnapshot, widgetId: string): IJOTAction[] | null {
    const layout = snapshot.widgetInstallations.layout!;
    const layoutIndex = layout.findIndex(item => item.id === widgetId);

    return [
      {
        n: OTActionName.ListDelete,
        p: ['widgetInstallations', 'layout', layoutIndex],
        ld: layout[layoutIndex],
      },
    ];
  }

  // 调整小组件的布局;
  static changeWidgetLayout2Action(snapshot: IDashboardSnapshot, options: { layout: IDashboardLayout[] }): IJOTAction[] | null {
    const oldLayout = snapshot.widgetInstallations.layout;
    const { layout } = options;

    if (layout.length !== oldLayout?.length) {
      return null;
    }

    const actions: IJOTAction[] = [];

    oldLayout.map((oldPosition, index) => {
      const newPosition = layout[index];
      for (const k in oldPosition) {
        actions.push({
          n: OTActionName.ObjectReplace,
          p: ['widgetInstallations', 'layout', index, k],
          oi: newPosition[k],
          od: oldPosition[k],
        });
      }
    });
    return actions;
  }
}
