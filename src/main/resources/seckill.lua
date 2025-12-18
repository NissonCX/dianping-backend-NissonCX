-- 秒杀脚本：校验库存、一人一单，并将订单信息发送到Redis Stream
local voucherId = ARGV[1]
local userId = ARGV[2]
local orderId = ARGV[3]

local stockKey = 'seckill:stock:' .. voucherId
local orderKey = 'seckill:order:' .. voucherId

-- 1.判断库存是否充足
local stock = redis.call('get', stockKey)
local stockNum = tonumber(stock)
if (stock == nil or stockNum == nil or stockNum <= 0) then
    -- 库存不足或不存在
    return 1
end

-- 2.判断用户是否已经下过单（一人一单）
if (redis.call('sismember', orderKey, userId) == 1) then
    -- 用户已经下过单
    return 2
end

-- 3.扣减库存
redis.call('decr', stockKey)
-- 4.记录用户已下单
redis.call('sadd', orderKey, userId)
-- 5.将订单信息发送到Redis Stream消息队列
redis.call('xadd', 'stream.orders', '*', 'userId', userId, 'voucherId', voucherId, 'id', orderId)

return 0
