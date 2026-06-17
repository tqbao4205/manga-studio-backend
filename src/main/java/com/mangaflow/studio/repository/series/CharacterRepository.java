package com.mangaflow.studio.repository.series;

import com.mangaflow.studio.model.series.Character;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * ── CharacterRepository ──
 * Repository cho entity Character — thao tác với bảng "characters".
 *
 * 📌 JpaRepository cung cấp sẵn:
 *    - findAll(), findById(), save(), deleteById(), ...
 *    - Tất cả đều có Transactional mặc định.
 *
 * 📌 findBySeriesId():
 *    Spring Data JPA tự động sinh query dựa trên tên method:
 *    "SELECT c FROM Character c WHERE c.series.id = ?1"
 *    Dùng để lấy danh sách characters của 1 series.
 *
 * 📌 findByIdAndSeriesId():
 *    "SELECT c FROM Character c WHERE c.id = ?1 AND c.series.id = ?2"
 *    Dùng để verify character có thuộc series không (tránh update/delete nhầm series).
 */
public interface CharacterRepository extends JpaRepository<Character, Long> {

    List<Character> findBySeriesId(Long seriesId);
}
