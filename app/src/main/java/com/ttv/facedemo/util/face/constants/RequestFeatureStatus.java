package com.ttv.facedemo.util.face.constants;

public @interface RequestFeatureStatus {
    int DEFAULT = -1;
    int SEARCHING = 0;
    int SUCCEED = 1;
    int TO_RETRY = 2;
    int FAILED = 3;
}
