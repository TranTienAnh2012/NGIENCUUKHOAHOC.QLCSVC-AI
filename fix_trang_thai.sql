-- Fix trang thai thiet bi dua theo ket qua bao tri cuoi cung
-- Loa JBL EON615 (id=5): bao_tri id=1 THANH_CONG -> TOT
UPDATE thiet_bi SET trang_thai = 'TOT', updated_at = NOW() WHERE id = 5;

-- Laptop Dell Latitude 5420 (id=3): bao_tri id=2 THANH_CONG -> TOT
UPDATE thiet_bi SET trang_thai = 'TOT', updated_at = NOW() WHERE id = 3;

-- Kiem tra ket qua
SELECT id, ten_thiet_bi, trang_thai FROM thiet_bi WHERE id IN (1, 3, 5);
