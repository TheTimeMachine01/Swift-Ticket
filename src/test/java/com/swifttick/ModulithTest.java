package com.swifttick;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModulithTest {

    @Test
    void verifyModules() {
        ApplicationModules modules = ApplicationModules.of(SwiftTickApplication.class);
        modules.verify();
    }
}
