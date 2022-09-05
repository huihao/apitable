import { FC, useState } from 'react';
import { ConfigConstant, Strings, t } from '@vikadata/core';
import { Modal } from 'pc/components/common/modal/modal';
import { Tooltip } from 'pc/components/common/tooltip';
import { Permission } from './permission';
import { PermissionDescModal } from 'pc/components/space_manage/workbench/permission_desc';
import styles from './style.module.less';
import { Popup } from 'pc/components/common/mobile/popup';
import { ComponentDisplay, ScreenSize } from 'pc/components/common/component_display';
import { getNodeIcon } from '../tree/node_icon';
import { PermissionModalHeader } from 'pc/components/field_permission/permission_modal_header';
import { InformationSmallOutlined } from '@vikadata/icons';
import { useThemeColors } from '@vikadata/components';

export interface IPermissionSettingsProps {
  data: {
    nodeId: string;
    name: string;
    type: ConfigConstant.NodeType;
    icon: string;
  };
  visible: boolean;
  onClose: () => void;
}

export const PermissionSettings: FC<IPermissionSettingsProps> = ({ data, visible, onClose }) => {
  const [permDescModalVisible, setPermDescModalVisible] = useState(false);
  const colors = useThemeColors();
  if (!visible) {
    return <></>;
  }

  const Title = () => {
    return (
      <Tooltip title={t(Strings.instruction_of_node_permission)}>
        <span className={styles.helpBtn} onClick={() => setPermDescModalVisible(true)}>
          <InformationSmallOutlined color={colors.thirdLevelText} className={styles.infoIcon} />
        </span>
      </Tooltip>
    );
  };

  return (
    <>
      <ComponentDisplay maxWidthCompatible={ScreenSize.md}>
        {visible && (
          <Popup
            className={styles.permissionDrawer}
            height="90%"
            visible={visible}
            placement="bottom"
            title={
              <PermissionModalHeader
                typeName={t(Strings.file)}
                targetName={data.name}
                targetIcon={getNodeIcon(data.icon, data.type)}
                docIcon={<Title />}
              />
            }
            onClose={() => onClose()}
            push={{ distance: 0 }}
            destroyOnClose
          >
            <Permission data={data} />
          </Popup>
        )}
      </ComponentDisplay>
      <ComponentDisplay minWidthCompatible={ScreenSize.md}>
        {visible && (
          <Modal
            visible
            title={
              <PermissionModalHeader
                typeName={t(Strings.file)}
                targetName={data.name}
                targetIcon={getNodeIcon(data.icon, data.type)}
                docIcon={<Title />}
              />
            }
            bodyStyle={{ padding: '0 0 24px 24px' }}
            width={560}
            onCancel={onClose}
            destroyOnClose
            footer={null}
            className={styles.permissionModal}
            centered
          >
            <>
              <Permission data={data} />
              {permDescModalVisible && <PermissionDescModal visible onCancel={() => setPermDescModalVisible(false)} />}
            </>
          </Modal>
        )}
      </ComponentDisplay>
    </>
  );
};
