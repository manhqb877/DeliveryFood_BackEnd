package com.fooddelivery.core.repository;

import com.fooddelivery.core.entity.CategoryOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface CategoryOptionRepository extends JpaRepository<CategoryOption, Long> {

    @Query("SELECT o FROM CategoryOption o WHERE o.category.id = :categoryId AND o.isActive = true")
    List<CategoryOption> findByCategoryId(@Param("categoryId") Long categoryId);

    @Modifying
    @Transactional
    @Query("DELETE FROM CategoryOption o WHERE o.category.id = :categoryId AND o.groupName = :groupName AND o.optionName = :optionName")
    void deleteByCategoryAndNames(@Param("categoryId") Long categoryId, @Param("groupName") String groupName, @Param("optionName") String optionName);

    @Modifying
    @Transactional
    @Query("UPDATE CategoryOption o SET o.groupName = :newGroup, o.optionName = :newOption, o.extraPrice = :newPrice, o.isRequired = :req, o.isMultiple = :mult, o.maxSelect = :maxSel, o.sortOrder = :sortOrd WHERE o.category.id = :categoryId AND o.groupName = :oldGroup AND o.optionName = :oldOption")
    void updateByCategoryAndNames(
            @Param("categoryId") Long categoryId,
            @Param("oldGroup") String oldGroup,
            @Param("oldOption") String oldOption,
            @Param("newGroup") String newGroup,
            @Param("newOption") String newOption,
            @Param("newPrice") BigDecimal newPrice,
            @Param("req") Boolean isRequired,
            @Param("mult") Boolean isMultiple,
            @Param("maxSel") Short maxSelect,
            @Param("sortOrd") Short sortOrder
    );
}
