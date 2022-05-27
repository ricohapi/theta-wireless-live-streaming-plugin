package com.theta360.pluginlibrary.values;

import com.theta360.pluginlibrary.activity.ThetaInfo;

public enum ThetaModel {
    THETA_V("RICOH THETA V"),
    THETA_Z1("RICOH THETA Z1"),
    THETA_X("RICOH THETA X"),
    THETA_DEF("RICOH THETA");

    private final String mModelName;

    ThetaModel(final String modelName) {
        this.mModelName = modelName;
    }

    public static ThetaModel getValue(String _modelName) {
        for (ThetaModel thetaModel : ThetaModel.values()) {
            if (thetaModel.toString().equals(_modelName)) {
                return thetaModel;
            }
        }
        return THETA_DEF;
    }

    public static Boolean isZ1Model() {
        ThetaModel model =ThetaModel.getValue(ThetaInfo.getThetaModelName());
        if (model == ThetaModel.THETA_Z1) {
            return true;
        } else {
            return false;
        }
    }

    public static Boolean isVCameraModel() {
        ThetaModel model =ThetaModel.getValue(ThetaInfo.getThetaModelName());
        if (model == ThetaModel.THETA_Z1 || model == ThetaModel.THETA_V) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return this.mModelName;
    }
}
