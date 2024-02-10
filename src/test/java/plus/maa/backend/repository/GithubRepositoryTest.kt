package plus.maa.backend.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import plus.maa.backend.config.external.MaaCopilotProperties;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class GithubRepositoryTest {

    @Autowired
    private GithubRepository repository;
    @Autowired
    private MaaCopilotProperties properties;

    @Test
    public void testGetTrees() {
        var maaTree = repository.getTrees(properties.getGithub().getToken(), "d989739981db071e80df1c66e473c729b50e8073");
        assertNotNull(maaTree);
    }

    @Test
    public void testGetCommits() {
        var commits = repository.getCommits(properties.getGithub().getToken());
        assertNotNull(commits);
        assertFalse(commits.isEmpty());
    }

    @Test
    void testGetContents() {
        var contents = repository.getContents(properties.getGithub().getToken(),"");
        assertNotNull(contents);
        assertFalse(contents.isEmpty());
    }
}