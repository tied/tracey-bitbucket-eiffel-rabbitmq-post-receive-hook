package net.praqma.stash.plugins.tracey.components.api;

import com.atlassian.stash.repository.RefChange;
import com.atlassian.stash.repository.Repository;

import java.util.Set;

public interface GitService {

    /**
     * Get new commits that are not already present in the repository for the given {@link RefChange}.
     * @param {@link Repository} is a repository to look at
     * @param refChange {@link RefChange} to start from
     * @return A {@link Set} of sha1's for new commits
     */
    Set<String> getCommitsDelta(Repository repository, RefChange refChange);

}