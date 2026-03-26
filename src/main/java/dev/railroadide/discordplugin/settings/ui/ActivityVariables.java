package dev.railroadide.discordplugin.settings.ui;

import dev.railroadide.railroad.ide.runconfig.RunConfiguration;
import dev.railroadide.railroad.localization.L18n;
import dev.railroadide.railroad.plugin.defaults.FileSystemDocument;
import dev.railroadide.railroad.plugin.spi.dto.Project;
import dev.railroadide.railroad.plugin.spi.state.Cursor;
import dev.railroadide.railroad.project.facet.data.BuildToolFacet;
import dev.railroadide.railroad.project.facet.data.MinecraftModFacetData;
import dev.railroadide.railroad.vcs.git.commit.GitCommit;
import dev.railroadide.railroad.vcs.git.remote.GitRemote;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ActivityVariables {
    private static final Map<String, ActivityVariable> VARIABLES_BY_KEY = new HashMap<>();

    private ActivityVariables() {
    }

    // Application
    public static final ActivityVariable APPLICATION_NAME = register(new ActivityVariable("application_name", "discord.settings.display_content.first_line.application.name", "discord.settings.display_content.first_line.application.name.description", VariableContext.APPLICATION, ctx -> ctx.getApplicationInfoService().getName()));
    public static final ActivityVariable APPLICATION_VERSION = register(new ActivityVariable("application_version", "discord.settings.display_content.first_line.application.version", "discord.settings.display_content.first_line.application.version.description", VariableContext.APPLICATION, ctx -> ctx.getApplicationInfoService().getVersion()));
    public static final ActivityVariable APPLICATION_BUILD_TIMESTAMP = register(new ActivityVariable("application_build_timestamp", "discord.settings.display_content.first_line.application.build_timestamp", "discord.settings.display_content.first_line.application.build_timestamp.description", VariableContext.APPLICATION, ctx -> String.valueOf(ctx.getApplicationInfoService().getBuildTimestamp())));

    // Project
    public static final ActivityVariable PROJECT_NAME = register(new ActivityVariable("project_name", "discord.settings.display_content.first_line.project.name", "discord.settings.display_content.first_line.project.name.description",
            VariableContext.PROJECT, ctx -> ctx.getProject() != null ? ctx.getProject().getAlias() : null));
    public static final ActivityVariable PROJECT_DESCRIPTION = register(new ActivityVariable("project_description", "discord.settings.display_content.first_line.project.description", "discord.settings.display_content.first_line.project.description.description",
            VariableContext.PROJECT, ctx -> ctx.getProject() != null ? ctx.getProject().getDescription() : null));
    public static final ActivityVariable PROJECT_PATH = register(new ActivityVariable("project_path", "discord.settings.display_content.first_line.project.path", "discord.settings.display_content.first_line.project.path.description",
            VariableContext.PROJECT, ctx -> ctx.getProject() != null ? ctx.getProject().getPath().toString() : null));
    public static final ActivityVariable PROJECT_LICENSE = register(new ActivityVariable("project_license", "discord.settings.display_content.first_line.project.license", "discord.settings.display_content.first_line.project.license.description",
            VariableContext.PROJECT, ctx -> ctx.getProject() != null ? ctx.getProject().getLicense().getName() : null));
    public static final ActivityVariable GIT_URL = register(new ActivityVariable("git_url", "discord.settings.display_content.first_line.project.gitUrl", "discord.settings.display_content.first_line.project.gitUrl.description",
            VariableContext.PROJECT, ctx -> {
                Project project = ctx.getProject();
                if (project == null)
                    return null;

                GitRemote currentRemote = project.getGitManager().getCurrentRemote();
                if (currentRemote == null)
                    return null;

                return currentRemote.fetchUrl() != null ? currentRemote.fetchUrl() : currentRemote.pushUrl();
            }));
    public static final ActivityVariable GIT_BRANCH = register(new ActivityVariable("git_branch", "discord.settings.display_content.first_line.project.gitBranch", "discord.settings.display_content.first_line.project.gitBranch.description",
            VariableContext.PROJECT, ctx -> ctx.getProject() != null ? ctx.getProject().getGitManager() != null ? ctx.getProject().getGitManager().getCurrentBranch() : null : null));
    public static final ActivityVariable GIT_COMMIT_HASH = register(new ActivityVariable("git_commit_hash", "discord.settings.display_content.first_line.project.commitHash", "discord.settings.display_content.first_line.project.commitHash.description",
            VariableContext.PROJECT, ctx -> ctx.getProject() != null ? ctx.getProject().getGitManager() != null ? ctx.getProject().getGitManager().getHeadCommitHash() : null : null));
    public static final ActivityVariable GIT_COMMIT_MESSAGE = register(new ActivityVariable("git_commit_message", "discord.settings.display_content.first_line.project.commitMessage", "discord.settings.display_content.first_line.project.commitMessage.description",
            VariableContext.PROJECT, ctx -> ctx.getProject() != null ? ctx.getProject().getGitManager() != null ? ctx.getProject().getGitManager().getCurrentCommit().map(GitCommit::subject).orElse("") : null : null));
    public static final ActivityVariable GIT_COMMIT_AUTHOR = register(new ActivityVariable("git_commit_author", "discord.settings.display_content.first_line.project.commitAuthor", "discord.settings.display_content.first_line.project.commitAuthor.description",
            VariableContext.PROJECT, ctx -> ctx.getProject() != null ? ctx.getProject().getGitManager() != null ? ctx.getProject().getGitManager().getCurrentCommit().map(GitCommit::authorName).orElse("") : null : null));
    public static final ActivityVariable GIT_COMMIT_TIME = register(new ActivityVariable("git_commit_time", "discord.settings.display_content.first_line.project.commitTime", "discord.settings.display_content.first_line.project.commitTime.description",
            VariableContext.PROJECT, ctx -> ctx.getProject() != null ? ctx.getProject().getGitManager() != null ? String.valueOf(ctx.getProject().getGitManager().getCurrentCommit().map(commit -> commit.authorTimestampEpochSeconds() * 1000L).orElse(0L)) : null : null));
    public static final ActivityVariable BUILD_TOOL = register(new ActivityVariable("build_tool", "discord.settings.display_content.first_line.project.buildTool", "discord.settings.display_content.first_line.project.buildTool.description", VariableContext.PROJECT, ctx -> {
        Project project = ctx.getProject();
        if (project == null)
            return null;

        List<String> buildTools = project.getFacets()
                .stream()
                .filter(facet -> facet.getData() instanceof BuildToolFacet)
                .map(facet -> facet.getType().name())
                .toList();

        if (buildTools.isEmpty())
            return null;

        return String.join(", ", buildTools);
    }));
    public static final ActivityVariable MOD_LOADER = register(new ActivityVariable("mod_loader", "discord.settings.display_content.first_line.project.modloader", "discord.settings.display_content.first_line.project.modloader.description", VariableContext.PROJECT, ctx -> {
        Project project = ctx.getProject();
        if (project == null)
            return null;

        List<String> modLoaders = project.getFacets()
                .stream()
                .filter(facet -> facet.getData() instanceof MinecraftModFacetData)
                .map(facet -> facet.getType().name())
                .toList();

        if (modLoaders.isEmpty())
            return null;

        return String.join(", ", modLoaders);
    }));
    public static final ActivityVariable MINECRAFT_VERSION = register(new ActivityVariable("minecraft_version", "discord.settings.display_content.first_line.project.minecraftVersion", "discord.settings.display_content.first_line.project.minecraftVersion.description", VariableContext.PROJECT, ctx -> {
        Project project = ctx.getProject();
        if (project == null)
            return null;

        List<String> minecraftVersions = project.getFacets()
                .stream()
                .filter(facet -> facet.getData() instanceof MinecraftModFacetData)
                .map(facet -> ((MinecraftModFacetData) facet.getData()).getMinecraftVersion())
                .toList();

        if (minecraftVersions.isEmpty())
            return null;

        return String.join(", ", minecraftVersions);
    }));
    public static final ActivityVariable LOADER_VERSION = register(new ActivityVariable("loader_version", "discord.settings.display_content.first_line.project.loaderVersion", "discord.settings.display_content.first_line.project.loaderVersion.description", VariableContext.PROJECT, ctx -> {
        Project project = ctx.getProject();
        if (project == null)
            return null;

        List<String> loaderVersions = project.getFacets()
                .stream()
                .filter(facet -> facet.getData() instanceof MinecraftModFacetData)
                .map(facet -> ((MinecraftModFacetData) facet.getData()).getLoaderVersion())
                .toList();

        if (loaderVersions.isEmpty())
            return null;

        return String.join(", ", loaderVersions);
    }));
    public static final ActivityVariable MOD_NAME = register(new ActivityVariable("mod_name", "discord.settings.display_content.first_line.project.modName", "discord.settings.display_content.first_line.project.modName.description", VariableContext.PROJECT, ctx -> {
        Project project = ctx.getProject();
        if (project == null)
            return null;

        List<String> modNames = project.getFacets()
                .stream()
                .filter(facet -> facet.getData() instanceof MinecraftModFacetData)
                .map(facet -> ((MinecraftModFacetData) facet.getData()).getDisplayName())
                .toList();

        if (modNames.isEmpty())
            return null;

        return String.join(", ", modNames);
    }));
    public static final ActivityVariable MOD_VERSION = register(new ActivityVariable("mod_version", "discord.settings.display_content.first_line.project.modVersion", "discord.settings.display_content.first_line.project.modVersion.description", VariableContext.PROJECT, ctx -> {
        Project project = ctx.getProject();
        if (project == null)
            return null;

        List<String> modVersions = project.getFacets()
                .stream()
                .filter(facet -> facet.getData() instanceof MinecraftModFacetData)
                .map(facet -> ((MinecraftModFacetData) facet.getData()).getVersion())
                .toList();

        if (modVersions.isEmpty())
            return null;

        return String.join(", ", modVersions);
    }));
    public static final ActivityVariable MOD_ID = register(new ActivityVariable("mod_id", "discord.settings.display_content.first_line.project.modId", "discord.settings.display_content.first_line.project.modId.description", VariableContext.PROJECT, ctx -> {
        Project project = ctx.getProject();
        if (project == null)
            return null;

        List<String> modIds = project.getFacets()
                .stream()
                .filter(facet -> facet.getData() instanceof MinecraftModFacetData)
                .map(facet -> ((MinecraftModFacetData) facet.getData()).getModId())
                .toList();

        if (modIds.isEmpty())
            return null;

        return String.join(", ", modIds);
    }));
    public static final ActivityVariable DEBUGGER_ACTIVE = register(new ActivityVariable("debugger_active", "discord.settings.display_content.first_line.project.debuggerActive", "discord.settings.display_content.first_line.project.debuggerActive.description",
            VariableContext.PROJECT, ctx -> ctx.getProject() != null ? String.valueOf(ctx.getProject().getDebuggingManager().isActive()) : null));
    public static final ActivityVariable DEBUGGER_PAUSED = register(new ActivityVariable("debugger_paused", "discord.settings.display_content.first_line.project.debuggerPaused", "discord.settings.display_content.first_line.project.debuggerPaused.description",
            VariableContext.PROJECT, ctx -> ctx.getProject() != null ? String.valueOf(ctx.getProject().getDebuggingManager().isPaused()) : null));
    public static final ActivityVariable RUN_CONFIGURATION_NAME = register(new ActivityVariable("run_configuration_name", "discord.settings.display_content.first_line.project.runConfigurationName", "discord.settings.display_content.first_line.project.runConfigurationName.description", VariableContext.PROJECT, ctx -> {
        Project project = ctx.getProject();
        if (project == null)
            return null;

        List<RunConfiguration<?>> configurations = project.getRunConfigManager().getConfigurations();
        if (configurations.isEmpty())
            return null;

        return configurations.stream()
                .filter(runConfiguration -> runConfiguration.isRunning(project))
                .findFirst()
                .map(config -> config.data().getName())
                .orElse(null);
    }));
    public static final ActivityVariable RUN_CONFIGURATION_TYPE = register(new ActivityVariable("run_configuration_type", "discord.settings.display_content.first_line.project.runConfigurationType", "discord.settings.display_content.first_line.project.runConfigurationType.description", VariableContext.PROJECT, ctx -> {
        Project project = ctx.getProject();
        if (project == null)
            return null;

        List<RunConfiguration<?>> configurations = project.getRunConfigManager().getConfigurations();
        if (configurations.isEmpty())
            return null;

        return configurations.stream()
                .filter(runConfiguration -> runConfiguration.isRunning(project))
                .findFirst()
                .map(config -> L18n.localize(config.data().getType().getLocalizationKey()))
                .orElse(null);
    }));
    public static final ActivityVariable STARTED_RUNNING_TIME = register(new ActivityVariable("started_running_time", "discord.settings.display_content.first_line.project.startedRunningTime", "discord.settings.display_content.first_line.project.startedRunningTime.description", VariableContext.PROJECT, ctx -> {
        Project project = ctx.getProject();
        if (project == null)
            return null;

        List<RunConfiguration<?>> configurations = project.getRunConfigManager().getConfigurations();
        if (configurations.isEmpty())
            return null;

        return configurations.stream()
                .filter(runConfiguration -> runConfiguration.isRunning(project))
                .findFirst()
                .map(config -> "0"/* TODO: Implement RunConfiguration#getStartTime in Railroad*/)
                .orElse(null);
    }));

    // Document
    public static final ActivityVariable DOCUMENT_NAME = register(new ActivityVariable("document_name", "discord.settings.display_content.first_line.document.name", "discord.settings.display_content.first_line.document.name.description",
            VariableContext.DOCUMENT, ctx -> ctx.getDocument() != null ? ctx.getDocument().getName() : null));
    public static final ActivityVariable DOCUMENT_PATH = register(new ActivityVariable("document_path", "discord.settings.display_content.first_line.document.path", "discord.settings.display_content.first_line.document.path.description",
            VariableContext.DOCUMENT, ctx -> ctx.getDocument() != null ? ctx.getDocument().getPath().toString() : null));
    public static final ActivityVariable DOCUMENT_TYPE = register(new ActivityVariable("document_type", "discord.settings.display_content.first_line.document.type", "discord.settings.display_content.first_line.document.type.description",
            VariableContext.DOCUMENT, ctx -> ctx.getDocument() != null ? ctx.getDocument().getPath().getFileName().toString().contains(".") ? ctx.getDocument().getPath().getFileName().toString().substring(ctx.getDocument().getPath().getFileName().toString().lastIndexOf(".") + 1) : null : null));
    public static final ActivityVariable DOCUMENT_LANGUAGE = register(new ActivityVariable("document_language", "discord.settings.display_content.first_line.document.language", "discord.settings.display_content.first_line.document.language.description",
            VariableContext.DOCUMENT, ctx -> ctx.getDocument() != null ? ctx.getDocument().getLanguageId() : null));
    public static final ActivityVariable DOCUMENT_LINE_COUNT = register(new ActivityVariable("document_line_count", "discord.settings.display_content.first_line.document.lineCount", "discord.settings.display_content.first_line.document.lineCount.description",
            VariableContext.DOCUMENT, ctx -> ctx.getDocument() != null ? String.valueOf(ctx.getDocument().getLineCount()) : null));
    public static final ActivityVariable DOCUMENT_CARET_LINE = register(new ActivityVariable("document_caret_line", "discord.settings.display_content.first_line.document.caretLine", "discord.settings.display_content.first_line.document.caretLine.description",
            VariableContext.DOCUMENT, ctx -> ctx.getDocument() != null ? String.valueOf(ctx.getCarets().stream().findFirst().map(Cursor::line).orElse(0)) : null));
    public static final ActivityVariable DOCUMENT_CARET_COLUMN = register(new ActivityVariable("document_caret_column", "discord.settings.display_content.first_line.document.caretColumn", "discord.settings.display_content.first_line.document.caretColumn.description",
            VariableContext.DOCUMENT, ctx -> ctx.getDocument() != null ? String.valueOf(ctx.getCarets().stream().findFirst().map(Cursor::column).orElse(0)) : null));
    public static final ActivityVariable DOCUMENT_CARET_OFFSET = register(new ActivityVariable("document_caret_offset", "discord.settings.display_content.first_line.document.caretOffset", "discord.settings.display_content.first_line.document.caretOffset.description",
            VariableContext.DOCUMENT, ctx -> "0" /* TODO: Implement ctx.getDocument() != null ? String.valueOf(ctx.getCarets().stream().findFirst().map(Cursor::pos).orElse(0)) : null*/));
    public static final ActivityVariable DOCUMENT_SIZE_BYTES = register(new ActivityVariable("document_size_bytes", "discord.settings.display_content.first_line.document.sizeBytes", "discord.settings.display_content.first_line.document.sizeBytes.description",
            VariableContext.DOCUMENT, ctx -> "0B"/* TODO: Add Document#getSizeInBytes in Railroad*/));
    public static final ActivityVariable DOCUMENT_SIZE_HUMAN = register(new ActivityVariable("document_size_human", "discord.settings.display_content.first_line.document.sizeHuman", "discord.settings.display_content.first_line.document.sizeHuman.description",
            VariableContext.DOCUMENT, ctx -> "0B"/* TODO: Add Document#getSizeInBytes in Railroad and convert to human-readable format*/));
    public static final ActivityVariable DOCUMENT_IS_DIRTY = register(new ActivityVariable("document_is_dirty", "discord.settings.display_content.first_line.document.isDirty", "discord.settings.display_content.first_line.document.isDirty.description",
            VariableContext.DOCUMENT, ctx -> ctx.getDocument() != null ? String.valueOf(ctx.getDocument().isDirty()) : null));
    public static final ActivityVariable DOCUMENT_IS_READONLY = register(new ActivityVariable("document_is_readonly", "discord.settings.display_content.first_line.document.isReadonly", "discord.settings.display_content.first_line.document.isReadonly.description",
            VariableContext.DOCUMENT, ctx -> "false"/* TODO: Implement Document#isReadonly in Railroad*/));
    public static final ActivityVariable DOCUMENT_IS_BINARY = register(new ActivityVariable("document_is_binary", "discord.settings.display_content.first_line.document.isBinary", "discord.settings.display_content.first_line.document.isBinary.description",
            VariableContext.DOCUMENT, ctx -> "false"/* TODO: Implement Document#isBinary in Railroad*/));
    public static final ActivityVariable DOCUMENT_IS_FILE = register(new ActivityVariable("document_is_file", "discord.settings.display_content.first_line.document.isFile", "discord.settings.display_content.first_line.document.isFile.description",
            VariableContext.DOCUMENT, ctx -> String.valueOf(ctx.getDocument() instanceof FileSystemDocument)));

    public static ActivityVariable getByKey(String key) {
        return VARIABLES_BY_KEY.get(key);
    }

    private static <T extends ActivityVariable> T register(T variable) {
        VARIABLES_BY_KEY.put(variable.key(), variable);
        return variable;
    }
}
