-- Token Bucket Rate Limiting for BIST Trading Platform
-- Arguments: current_time, requests_per_second, burst_capacity, tokens_per_request
-- Returns: {allowed, remaining_tokens, retry_after_ms}

local key = KEYS[1]
local current_time = tonumber(ARGV[1])
local requests_per_second = tonumber(ARGV[2])
local burst_capacity = tonumber(ARGV[3])
local tokens_per_request = tonumber(ARGV[4])

-- Get current bucket state
local bucket_data = redis.call('HMGET', key, 'tokens', 'last_refill')
local current_tokens = tonumber(bucket_data[1]) or burst_capacity
local last_refill = tonumber(bucket_data[2]) or current_time

-- Calculate tokens to add based on time passed
local time_passed_ms = current_time - last_refill
local tokens_to_add = math.floor(time_passed_ms * requests_per_second / 1000)

-- Update token count (cannot exceed burst capacity)
current_tokens = math.min(burst_capacity, current_tokens + tokens_to_add)

-- Check if we have enough tokens for this request
if current_tokens < tokens_per_request then
    -- Not enough tokens, calculate retry after
    local tokens_needed = tokens_per_request - current_tokens
    local retry_after_ms = math.ceil(tokens_needed * 1000 / requests_per_second)

    -- Update bucket state without consuming tokens
    redis.call('HMSET', key, 'tokens', current_tokens, 'last_refill', current_time)
    redis.call('EXPIRE', key, 3600) -- 1 hour expiry for cleanup

    return {0, current_tokens, retry_after_ms}
end

-- Consume tokens
current_tokens = current_tokens - tokens_per_request

-- Update bucket state
redis.call('HMSET', key, 'tokens', current_tokens, 'last_refill', current_time)
redis.call('EXPIRE', key, 3600) -- 1 hour expiry for cleanup

return {1, current_tokens, 0}