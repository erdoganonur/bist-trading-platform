-- Sliding Window Rate Limiting for BIST Trading Platform
-- Arguments: current_time, window_size_ms, sub_window_size_ms, requests_per_second,
--           burst_capacity, tokens_per_request, number_of_sub_windows
-- Returns: {allowed, remaining_tokens, retry_after_ms, total_requests}

local key = KEYS[1]
local current_time = tonumber(ARGV[1])
local window_size_ms = tonumber(ARGV[2])
local sub_window_size_ms = tonumber(ARGV[3])
local requests_per_second = tonumber(ARGV[4])
local burst_capacity = tonumber(ARGV[5])
local tokens_per_request = tonumber(ARGV[6])
local number_of_sub_windows = tonumber(ARGV[7])

-- Calculate window boundaries
local window_start = current_time - window_size_ms
local current_sub_window = math.floor(current_time / sub_window_size_ms)

-- Clean up old sub-windows
local cleanup_threshold = current_time - window_size_ms - sub_window_size_ms
redis.call('ZREMRANGEBYSCORE', key, '-inf', cleanup_threshold)

-- Get current request count in the sliding window
local total_requests = 0
local sub_window_counts = redis.call('ZRANGE', key, 0, -1, 'WITHSCORES')

-- Calculate total requests in sliding window
for i = 1, #sub_window_counts, 2 do
    local timestamp = tonumber(sub_window_counts[i + 1])
    if timestamp >= window_start then
        total_requests = total_requests + tonumber(sub_window_counts[i])
    end
end

-- Check if request would exceed burst capacity
if total_requests + tokens_per_request > burst_capacity then
    -- Calculate retry after based on oldest request in window
    local oldest_in_window = redis.call('ZRANGE', key, 0, 0, 'WITHSCORES')
    local retry_after_ms = 0

    if #oldest_in_window >= 2 then
        local oldest_timestamp = tonumber(oldest_in_window[2])
        retry_after_ms = math.max(0, oldest_timestamp + window_size_ms - current_time)
    else
        retry_after_ms = window_size_ms
    end

    -- Set expiry for cleanup
    redis.call('EXPIRE', key, math.ceil(window_size_ms / 1000))

    return {0, 0, retry_after_ms, total_requests}
end

-- Check sustainable rate (requests per second)
local sustained_limit = math.floor(requests_per_second * (window_size_ms / 1000))
if total_requests + tokens_per_request > sustained_limit then
    -- Allow burst but calculate penalty
    local overage = (total_requests + tokens_per_request) - sustained_limit
    local penalty_ms = math.floor(overage * 1000 / requests_per_second)

    -- Still allow the request but with longer retry after
    redis.call('ZADD', key, current_time, current_sub_window .. ':' .. tokens_per_request)
    redis.call('EXPIRE', key, math.ceil(window_size_ms / 1000) + 10)

    local remaining = math.max(0, burst_capacity - total_requests - tokens_per_request)
    return {1, remaining, penalty_ms, total_requests + tokens_per_request}
end

-- Request is within limits, allow it
redis.call('ZADD', key, current_time, current_sub_window .. ':' .. tokens_per_request)
redis.call('EXPIRE', key, math.ceil(window_size_ms / 1000) + 10)

local remaining = math.max(0, burst_capacity - total_requests - tokens_per_request)
return {1, remaining, 0, total_requests + tokens_per_request}