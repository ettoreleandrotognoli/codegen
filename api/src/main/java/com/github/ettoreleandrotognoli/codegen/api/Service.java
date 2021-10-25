package com.github.ettoreleandrotognoli.codegen.api;

import java.util.Map;

public interface Service {

    Map<String, Class<? extends RawCodeSpec>> getAliases();
}
