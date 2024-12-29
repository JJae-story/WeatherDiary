package zerobase.weather;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class WeatherApplicationTests {
	int a = 1;
	int b = 1;

	@Test
	void equalTest() {
		assertEquals(a, b);
	}

	@Test
	void nullTest() {
		assertNull(null);
	}

	@Test
	void trueTest() {
		assertTrue(1 == 1);
	}

}
