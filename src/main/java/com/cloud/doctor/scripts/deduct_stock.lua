-- KEYS[1]: 库存的 Key (例如: schedule:stock:5)
-- 返回值: 1=扣减成功, 0=库存不足, -1=Key不存在

--先检查在这个 Key 是否存在？
if (redis.call('exists', KEYS[1]) == 1) then

    --如果存在，把它取出来(get)，并转成数字(tonumber)
    local stock = tonumber(redis.call('get', KEYS[1]));

    --判断库存是不是大于 0？
    if (stock > 0) then
        --如果够，就执行减 1 操作 (decr)
        redis.call('decr', KEYS[1]);
        --返回 1，告诉 Java "抢到了"
        return 1;
    end

    --如果库存 <= 0，返回 0，告诉 Java "没抢到"
    return 0;
end

--如果 Key 根本不存在（没预热），返回 -1，告诉 Java "去查数据库"
return -1;