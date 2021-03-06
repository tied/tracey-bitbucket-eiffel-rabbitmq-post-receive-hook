package net.praqma.bitbucket.plugins.tracey.components.impl;

import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.server.ApplicationPropertiesService;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import net.praqma.bitbucket.plugins.tracey.components.api.ProtocolConfigurationService;
import net.praqma.bitbucket.plugins.tracey.components.api.ProtocolService;
import net.praqma.bitbucket.plugins.tracey.exceptions.ProtocolServiceException;
import net.praqma.tracey.protocol.eiffel.events.EiffelSourceChangeCreatedEventOuterClass.EiffelSourceChangeCreatedEvent;
import net.praqma.tracey.protocol.eiffel.factories.EiffelSourceChangeCreatedEventFactory;
import net.praqma.tracey.protocol.eiffel.models.Models;
import net.praqma.utils.parsers.cmg.api.CommitMessageParser;
import net.praqma.utils.parsers.cmg.impl.Jira;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;

public class EiffelProtocolServiceImpl implements ProtocolService {
    private static final Logger LOG = LoggerFactory.getLogger(EiffelProtocolServiceImpl.class);
    private final ApplicationPropertiesService applicationPropertiesService;
    private final ProtocolConfigurationService protocolConfigurationService;

    public EiffelProtocolServiceImpl(final ApplicationPropertiesService applicationPropertiesService, final ProtocolConfigurationService protocolConfigurationService) {
        this.applicationPropertiesService = applicationPropertiesService;
        this.protocolConfigurationService = protocolConfigurationService;
    }

    @Override
    public String getMessage(final String commmitId, final String branch, final String jiraUrl, final String jiraProjectName, final Repository repository) throws ProtocolServiceException {
        final String repoPath = applicationPropertiesService.getRepositoryDir(repository).getAbsolutePath();
        final EiffelSourceChangeCreatedEventFactory factory = new EiffelSourceChangeCreatedEventFactory(
                this.applicationPropertiesService.getDisplayName(),
                this.applicationPropertiesService.getBaseUrl().toString(),
                ((EiffelProtocolConfigurationServiceImpl) this.protocolConfigurationService).getDomainId(),
                getGAV());
        String result;
        CommitMessageParser parser;
        try {
            parser = new Jira(new URL(jiraUrl), jiraProjectName);
        } catch (MalformedURLException error) {
            throw new ProtocolServiceException("Can't parse commit " + commmitId + ". Can't parse URL " + jiraUrl, error);
        }
        try {
            factory.parseFromGit(repoPath, commmitId, branch, parser);
        } catch (IOException error) {
            throw new ProtocolServiceException("Can't parse commit " + commmitId + " info from repository " + repoPath, error);
        }
        EiffelSourceChangeCreatedEvent.Builder event = (EiffelSourceChangeCreatedEvent.Builder) factory.create();
        // Update GitIdentifier
        LOG.debug("Generated message before GitIdentifier update: " + event.toString());
        EiffelSourceChangeCreatedEvent.EiffelSourceChangeCreatedEventData.Builder data = event.getData().toBuilder();
        Models.Data.GitIdentifier.Builder git = data.getGitIdentifier().toBuilder();
        git.setRepoName(repository.getSlug());
        git.setRepoUri(applicationPropertiesService.getBaseUrl().toString() + "/scm/" + repository.getProject().getKey() + "/" + repository.getSlug());
        LOG.debug("Set GitIdentifier to " + git.toString());
        event.setData(data.setGitIdentifier(git));
        try {
            result = JsonFormat.printer().print(event.build());
        } catch (InvalidProtocolBufferException error) {
            throw new ProtocolServiceException("Can't format message to JSON\n" + event.build().toString(), error);
        }
        return result;
    }

    private Models.Data.GAV getGAV(){
        final Models.Data.GAV.Builder gav = Models.Data.GAV.newBuilder();
        final Dictionary headers = FrameworkUtil.getBundle(this.getClass()).getHeaders();
        gav.setGroupId(headers.get("Bundle-SymbolicName").toString());
        gav.setArtifactId(headers.get("Bundle-Name").toString());
        gav.setVersion(headers.get("Bundle-Version").toString());
        return gav.build();
    }
}