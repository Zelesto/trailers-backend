package com.pgsa.trailers.config;

import com.pgsa.trailers.entity.finance.InvoiceItem;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Field;
import java.util.Arrays;

@Slf4j
@Configuration
public class EntityVersionProbe {

    @PostConstruct
    public void probe() {
        Table table = InvoiceItem.class.getAnnotation(Table.class);
        log.info("╔═══════════════════════════════════════════════════════════");
        log.info("║ 🔍 InvoiceItem entity probe");
        log.info("║ Class file:     {}", InvoiceItem.class.getProtectionDomain().getCodeSource());
        log.info("║ @Table name:    {}", table != null ? table.name() : "(none)");
        log.info("║ Mapped columns:");
        Arrays.stream(InvoiceItem.class.getDeclaredFields())
                .filter(f -> !java.lang.reflect.Modifier.isStatic(f.getModifiers()))
                .forEach(f -> {
                    Column c = f.getAnnotation(Column.class);
                    log.info("║   field='{}'  ->  column='{}'",
                            f.getName(), c != null ? c.name() : "(default)");
                });
        log.info("╚═══════════════════════════════════════════════════════════");
    }
}
