package com.netflix.nfsidecar.utils;

import java.io.IOException;

public interface ProcessTuner
{
    void writeAllProperties(String yamlLocation) throws Exception;

}
