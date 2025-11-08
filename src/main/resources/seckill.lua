local voucherId = ARGV[1]
local userId = ARGV[2]
local stockKey = 'seckill:stock:' .. voucherId
local orderKey = 'seckill:order:' .. voucherId

-- 修复：正确处理库存值的空值检查
local stock = redis.call('get', stockKey)
local stockNum = tonumber(stock)
if (stock == nil or stockNum == nil or stockNum <= 0) then
    -- 库存不足或不存在
    return 1
end

-- 修复：重复秒杀
if (redis.call('sismember', orderKey, userId) == 1) then
    -- 用户已经下过单
    return 2
end

-- 修复：库存超卖
redis.call('decr', stockKey)
redis.call('sadd', orderKey, userId)

return 0
