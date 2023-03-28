package plus.maa.backend.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import plus.maa.backend.config.external.MaaCopilotProperties;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class GithubRepositoryTest {

    @Autowired
    private GithubRepository repository;
    @Autowired
    private MaaCopilotProperties properties;

    static final String MAA_REPO = "MaaAssistantArknights/MaaAssistantArknights";

    @Test
    public void testGetTrees() {
        var maaTree = repository.getTrees(properties.getGithub().getToken(), MAA_REPO, "d989739981db071e80df1c66e473c729b50e8073");
        assertNotNull(maaTree);
    }

    @Test
    public void testGetCommits() {
        var commits = repository.getCommits(properties.getGithub().getToken(), MAA_REPO, "/src/Cpp", 10);
        assertNotNull(commits);
        assertTrue(commits.size() > 0 && commits.size() < 10);
    }

}