package dev.railroadide.discordplugin.settings.ui;

import dev.railroadide.railroad.plugin.spi.dto.Document;
import dev.railroadide.railroad.plugin.spi.dto.Project;
import dev.railroadide.railroad.plugin.spi.services.ApplicationInfoService;
import dev.railroadide.railroad.plugin.spi.state.Cursor;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class VariableFetchContext {
    private ApplicationInfoService applicationInfoService;
    private Project project;
    private Document document;
    private List<Cursor> carets;
}
