package zerobase.weather.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import zerobase.weather.domain.Memo;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class JdbcMemoRepositoryTest {

    @Autowired
    JdbcMemoRepository jdbcMemoRepository;

    @Test
    void insertMemoTest() {
        // given : 주어진 것을
        Memo newMemo = new Memo(2, "Insert Memo Test");

        // when : ~을 했을 때
        jdbcMemoRepository.save(newMemo);

        // then : 이러한 결과가 나와야 한다
        Optional<Memo> result = jdbcMemoRepository.findById(2);
        assertEquals(result.get().getText(), ("Insert Memo Test"));
    }

    @Test
    void findAllMemosTest() {
        List<Memo> memoList = jdbcMemoRepository.findAll();
        System.out.println(memoList);
        assertNotNull(memoList);
    }

}