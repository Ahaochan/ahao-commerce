-- 创建命名空间
create_namespace 'ORDER_NAMESPACE'

-- 在命名空间下创建表，并指定压缩算法、预分区以及rowKey散列策略
create 'ORDER_NAMESPACE:ORDER_SNAPSHOT',{NAME => "SNAPSHOT",COMPRESSION => "GZ"},{NUMREGIONS=>5,SPLITALGO=>'HexStringSplit'}
