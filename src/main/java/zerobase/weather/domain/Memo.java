package zerobase.weather.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "memo")
public class Memo {
    /**
     * GenerationType.AUTO : 상황에 맞춰서 자동으로 해줘
     * GenerationType.IDENTITY : 기본적인 생성을 데이터 베이스에 맡기겠다.
     * GenerationType.SEQUENCE : 데이터 베이스 오브젝트를 만들어서 그 오브젝트가 키 생성
     * GenerationType.TABLE : 키 생성 만을 위한 테이블을 만들어서 키 생성
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String text;
}
