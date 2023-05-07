package plus.maa.backend;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.mockito.MockitoAnnotations;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BaseMockTest {

    AutoCloseable closeable;

    @BeforeAll
    void initTest() {
        this.closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterAll
    void endTest() throws Exception {
        this.closeable.close();
    }

}
