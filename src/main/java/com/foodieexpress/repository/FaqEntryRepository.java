package com.foodieexpress.repository;
import com.foodieexpress.model.FaqEntry;import org.springframework.data.jpa.repository.JpaRepository;import java.util.*;
public interface FaqEntryRepository extends JpaRepository<FaqEntry,Long>{List<FaqEntry> findByActiveTrueOrderByCategoryAscIdAsc();List<FaqEntry> findAllByOrderByCategoryAscIdAsc();}
