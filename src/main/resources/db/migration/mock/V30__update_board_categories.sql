-- Board category 업데이트 스크립트
-- BoardCategoryVo에서 허용되지 않는 카테고리를 허용되는 카테고리로 변경

-- ADVERTISE 카테고리를 ACTIVITY로 변경
UPDATE board 
SET category = 'ACTIVITY' 
WHERE category = 'ADVERTISE';

-- INFORMATION 카테고리를 TECH로 변경  
UPDATE board 
SET category = 'TECH' 
WHERE category = 'INFORMATION';

