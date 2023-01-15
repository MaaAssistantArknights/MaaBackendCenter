package plus.maa.backend.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class GithubRepositoryTest {

    @Autowired
    GithubRepository repository;

    @Value("${maa-copilot.github.token:}")
    private String githubToken;

    static final String MAA_REPO = "MaaAssistantArknights/MaaAssistantArknights";

    @Test
    public void testGetTrees() {
        var maaTree = repository.getTrees(githubToken, MAA_REPO, "d989739981db071e80df1c66e473c729b50e8073");
        assertNotNull(maaTree);
    }

    @Test
    public void testGetCommits() {
        var commits = repository.getCommits(githubToken, MAA_REPO, "/src/Cpp", 10);
        assertNotNull(commits);
        assertTrue(commits.size() > 0 && commits.size() < 10);
    }

}