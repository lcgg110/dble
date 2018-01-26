package com.actiontech.dble.config.loader.ucoreprocess.loader;

import com.actiontech.dble.DbleServer;
import com.actiontech.dble.backend.mysql.view.Repository;
import com.actiontech.dble.config.loader.ucoreprocess.*;
import com.actiontech.dble.config.loader.ucoreprocess.bean.UKvBean;
import com.actiontech.dble.meta.ViewMeta;
import com.actiontech.dble.net.mysql.ErrorPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.actiontech.dble.config.loader.ucoreprocess.UcorePathUtil.SEPARATOR;

/**
 * Created by szf on 2018/2/5.
 */
public class UViewChildResponse implements UcoreXmlLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(UViewChildResponse.class);

    @Override
    public void notifyProcess(UKvBean configValue) throws Exception {
        if (configValue.getKey().split("/").length != 6) {
            return;
        }
        String schema = configValue.getKey().split("/")[5].split(Repository.SCHEMA_VIEW_SPLIT)[0];
        String viewName = configValue.getKey().split(Repository.SCHEMA_VIEW_SPLIT)[1];
        if ("".equals(configValue.getValue())) {
            //the value of key is empty,just doing nothing
            return;
        }
        String serverId = configValue.getValue().split(Repository.SCHEMA_VIEW_SPLIT)[0];
        String optionType = configValue.getValue().split(Repository.SCHEMA_VIEW_SPLIT)[1];
        String myId = UcoreConfig.getInstance().getValue(UcoreParamCfg.UCORE_CFG_MYID);
        if (myId.equals(serverId) || configValue.getChangeType() == UKvBean.DELETE) {
            // self node do noting
            return;
        } else {
            try {
                if (Repository.DELETE.equals(optionType)) {
                    if (!DbleServer.getInstance().getTmManager().getCatalogs().get(schema).getViewMetas().containsKey(viewName)) {
                        return;
                    }
                    DbleServer.getInstance().getTmManager().getCatalogs().get(schema).getViewMetas().remove(viewName);
                    ClusterUcoreSender.sendDataToUcore(configValue.getKey() + SEPARATOR + myId, UcorePathUtil.SUCCESS);
                } else if (Repository.UPDATE.equals(optionType)) {
                    String stmt = ClusterUcoreSender.getKey(UcorePathUtil.getViewPath() + SEPARATOR + schema + Repository.SCHEMA_VIEW_SPLIT + viewName).getValue();
                    if (DbleServer.getInstance().getTmManager().getCatalogs().get(schema).getViewMetas().get(viewName) != null &&
                            stmt.equals(DbleServer.getInstance().getTmManager().getCatalogs().get(schema).getViewMetas().get(viewName).getCreateSql())) {
                        return;
                    }
                    ViewMeta vm = new ViewMeta(stmt, schema, DbleServer.getInstance().getTmManager());
                    ErrorPacket error = vm.initAndSet(true);
                    if (error != null) {
                        ClusterUcoreSender.sendDataToUcore(configValue.getKey() + SEPARATOR + myId, new String(error.getMessage()));
                        return;
                    }
                    ClusterUcoreSender.sendDataToUcore(configValue.getKey() + SEPARATOR + myId, UcorePathUtil.SUCCESS);
                }
            } catch (Exception e) {
                ClusterUcoreSender.sendDataToUcore(configValue.getKey() + "/" + myId, e.toString());
            }
        }
    }

    @Override
    public void notifyProcessWithKey(String key, String value) throws Exception {
        return;
    }

    @Override
    public void notifyCluster() throws Exception {
        return;
    }
}
