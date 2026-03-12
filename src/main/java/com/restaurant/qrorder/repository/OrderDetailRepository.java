package com.restaurant.qrorder.repository;

import com.restaurant.qrorder.domain.common.ItemStatus;
import com.restaurant.qrorder.domain.entity.OrderDetail;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long>, JpaSpecificationExecutor {

    List<OrderDetail> findAll(Specification orderDetailSpecification);


}
