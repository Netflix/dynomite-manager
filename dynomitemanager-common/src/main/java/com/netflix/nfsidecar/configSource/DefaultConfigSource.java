package com.netflix.nfsidecar.configSource;

import javax.inject.Inject;


public class DefaultConfigSource extends CompositeConfigSource {

  @Inject
  public DefaultConfigSource(final PropertiesConfigSource simpleDBConfigSource,
                           final PropertiesConfigSource propertiesConfigSource,
                           final SystemPropertiesConfigSource systemPropertiesConfigSource) {
    super(simpleDBConfigSource, propertiesConfigSource, systemPropertiesConfigSource);
  }
}
