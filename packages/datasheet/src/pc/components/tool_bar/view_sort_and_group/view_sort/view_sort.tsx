import { CollaCommandName, FieldType, ISortInfo, Selectors, Strings, t } from '@vikadata/core';
import { Col, Row, Switch } from 'antd';
import produce from 'immer';
import { ComponentDisplay, ScreenSize } from 'pc/components/common/component_display';
import { useResponsive } from 'pc/hooks';
import { useCallback, useMemo, useRef } from 'react';
import * as React from 'react';
import { DropResult } from 'react-beautiful-dnd';
import { useSelector } from 'react-redux';
import { CommonViewSet } from '../common_view_set';
import styles from '../style.module.less';
import { ViewFieldOptions } from '../view_field_options';
import { ViewFieldOptionsMobile } from '../view_field_options/view_field_options_mobile';
import { SyncViewTip } from '../../sync_view_tip';
import { Button, IUseListenTriggerInfo, TextButton, Typography, useListenVisualHeight, useThemeColors } from '@vikadata/components';
import { InformationLargeOutlined } from '@vikadata/icons';
import { resourceService } from 'pc/resource_service';
import { executeCommandWithMirror } from 'pc/utils/execute_command_with_mirror';

interface IViewSetting {
  close(e: React.MouseEvent): void;
  triggerInfo?: IUseListenTriggerInfo;
}

const MIN_HEIGHT = 120;
const MAX_HEIGHT = 340;

export const ViewSort: React.FC<IViewSetting> = props => {
  const { triggerInfo } = props;
  const colors = useThemeColors();
  const activeViewGroupInfo = useSelector(Selectors.getActiveViewGroupInfo);
  const fieldMap = useSelector(state => {
    return Selectors.getFieldMap(state, state.pageParams.datasheetId!);
  })!;
  const sortInfo = useSelector(Selectors.getActiveViewSortInfo);
  const activityViewId = useSelector(Selectors.getActiveView)!;
  const sortFieldIds = sortInfo ? sortInfo.rules.map(item => item.fieldId) : [];
  const { screenIsAtMost } = useResponsive();
  const isMobile = screenIsAtMost(ScreenSize.md);
  const { editable } = useSelector(Selectors.getPermissions);

  const containerRef = useRef<HTMLDivElement | null>(null);
  const { style, onListenResize } = useListenVisualHeight({
    listenNode: containerRef,
    minHeight: MIN_HEIGHT,
    maxHeight: MAX_HEIGHT,
    triggerInfo,
  });

  function submitSort(viewInfo: ISortInfo, applySort?: boolean) {
    const sortInfo = viewInfo?.rules?.length ? viewInfo : undefined;
    const _applySort = applySort && editable;
    executeCommandWithMirror(
      () => {
        resourceService.instance!.commandManager.execute({
          cmd: CollaCommandName.SetSortInfo,
          viewId: activityViewId,
          data: sortInfo,
          applySort: _applySort,
        });
      },
      {
        sortInfo: sortInfo,
      },
      () => {
        if (_applySort) {
          resourceService.instance!.commandManager.execute({
            cmd: CollaCommandName.SetSortInfo,
            viewId: activityViewId,
            data: sortInfo,
            applySort: _applySort,
          });
        }
      },
    );
  }

  const invalidFieldsByGroup = useMemo(() => {
    const invalidFields: string[] = [];
    activeViewGroupInfo.forEach(item => {
      const field = fieldMap[item.fieldId];
      // 非多选 FieldType 分组后，排序无效
      if (field && ![FieldType.MultiSelect].includes(field.type)) {
        invalidFields.push(field.id);
      }
    });
    return invalidFields;
  }, [activeViewGroupInfo, fieldMap]);

  // 拖动结束之后修改顺序
  const onDragEnd = useCallback(
    (result: DropResult) => {
      const { source, destination } = result;
      if (!destination) {
        return;
      }
      submitSort(
        produce(sortInfo, draft => {
          draft!.rules.splice(destination.index, 0, draft!.rules.splice(source.index, 1)[0]);
          return draft;
        })!,
      );
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [sortInfo],
  );

  function deleteViewItem(index: number) {
    if (sortInfo) {
      const newSortInfo = produce(sortInfo, draft => {
        draft.rules.splice(index, 1);
        return draft;
      });
      submitSort(newSortInfo);
    }
  }

  function setSortRules(index: number, desc: boolean) {
    const newSortInfo = produce(sortInfo, draft => {
      draft!.rules.map((item, idx) => {
        if (idx === index) {
          item.desc = desc;
        }
        return item;
      });
      return draft!;
    });
    submitSort(newSortInfo);
  }

  function setSortField(index: number, fieldId: string) {
    const newSortIno = produce(sortInfo, draft => {
      if (!draft) {
        return {
          keepSort: true,
          rules: [{ fieldId, desc: false }],
        };
      }
      draft.rules[index] = { fieldId, desc: false };
      return draft;
    });
    submitSort(newSortIno);
  }

  function onChange(check: boolean) {
    const newSortInfo = {
      ...sortInfo!,
      keepSort: check,
    };
    submitSort(newSortInfo, check ? false : true);
  }

  // TODO: 重新布局
  const manualSort = sortInfo && !sortInfo.keepSort;
  const mainContentStyle: React.CSSProperties = isMobile
    ? {
        maxHeight: manualSort ? 'calc(100% - 104px)' : 'calc(100% - 36px)',
      }
    : {};

  React.useEffect(() => {
    onListenResize();
  }, [sortInfo, onListenResize]);

  return (
    <div className={styles.viewSort} style={!isMobile ? style : undefined} ref={containerRef}>
      <div className={styles.boxTop}>
        {!isMobile && (
          <div style={{ display: 'flex', alignItems: 'center' }}>
            <Typography variant={'h7'}>{t(Strings.set_sort)}</Typography>
            <a href={t(Strings.sort_help_url)} target="_blank" rel="noopener noreferrer">
              <InformationLargeOutlined color={colors.thirdLevelText} />
            </a>
          </div>
        )}
        {Boolean(sortInfo && sortInfo.rules.length) && (
          <div className={styles.keepSort}>
            {t(Strings.keep_sort)}
            <Switch checked={sortInfo!.keepSort} size={isMobile ? 'default' : 'small'} style={{ marginLeft: isMobile ? 8 : 4 }} onChange={onChange} />
          </div>
        )}
      </div>
      {!isMobile && <SyncViewTip style={{ paddingLeft: 20 }} />}
      <main style={mainContentStyle}>
        {sortInfo && (
          <CommonViewSet
            onDragEnd={onDragEnd}
            dragData={sortInfo.rules}
            setField={setSortField}
            existFieldIds={sortFieldIds}
            setRules={setSortRules}
            deleteItem={deleteViewItem}
            invalidFieldIds={invalidFieldsByGroup}
            invalidTip={t(Strings.invalid_action_sort_tip)}
          />
        )}
      </main>
      <div className={styles.selectField}>
        <ComponentDisplay minWidthCompatible={ScreenSize.md}>
          <ViewFieldOptions
            isAddNewOption
            onChange={setSortField.bind(null, sortFieldIds.length)}
            defaultFieldId={t(Strings.add_sort)}
            existFieldIds={sortFieldIds}
            invalidFieldIds={invalidFieldsByGroup}
            invalidTip={t(Strings.invalid_option_sort_tip)}
          />
        </ComponentDisplay>
        <ComponentDisplay maxWidthCompatible={ScreenSize.md}>
          <Row align="middle" style={{ width: '100%' }}>
            <Col span={sortInfo ? 9 : 10} offset={sortInfo ? 1 : 0}>
              <div style={{ paddingLeft: sortInfo ? 8 : 0, paddingRight: 8 }}>
                <ViewFieldOptionsMobile
                  defaultFieldId={t(Strings.sort_rules)}
                  existFieldIds={sortFieldIds}
                  onChange={setSortField.bind(null, sortFieldIds.length)}
                />
              </div>
            </Col>
          </Row>
        </ComponentDisplay>
      </div>
      {manualSort && (
        <div className={styles.buttonWrapper}>
          {isMobile ? (
            <Button
              style={{ marginRight: '16px' }}
              size="large"
              onClick={e => {
                props.close((e as any) as React.MouseEvent);
              }}
              block
            >
              <span style={{ color: colors.thirdLevelText }}>{t(Strings.cancel)}</span>
            </Button>
          ) : (
            <TextButton
              style={{ marginRight: '16px' }}
              size="small"
              onClick={e => {
                props.close((e as any) as React.MouseEvent);
              }}
            >
              <span style={{ color: colors.thirdLevelText }}>{t(Strings.cancel)}</span>
            </TextButton>
          )}
          <Button
            color="primary"
            size={isMobile ? 'large' : 'small'}
            onClick={e => {
              sortInfo && submitSort(sortInfo, true);
              props.close((e as any) as React.MouseEvent);
            }}
            block={isMobile}
          >
            <span>{t(Strings.sort_apply)}</span>
          </Button>
        </div>
      )}
    </div>
  );
};
