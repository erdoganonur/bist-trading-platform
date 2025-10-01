-- Fixed Window Rate Limiting for BIST Trading Platform
-- Arguments: current_time, window_size_ms, requests_per_second, burst_capacity, tokens_per_request
-- Returns: {allowed, remaining_tokens, retry_after_ms, total_requests}

local key = KEYS[1]
local current_time = tonumber(ARGV[1])
local window_size_ms = tonumber(ARGV[2])
local requests_per_second = tonumber(ARGV[3])
local burst_capacity = tonumber(ARGV[4])
local tokens_per_request = tonumber(ARGV[5])

-- Calculate current window start time
local window_start = math.floor(current_time / window_size_ms) * window_size_ms
local window_key = key .. ':' .. window_start

-- Get current request count in this window
local current_requests = redis.call('GET', window_key)
current_requests = tonumber(current_requests) or 0

-- Calculate window limits
local window_limit = math.min(burst_capacity, requests_per_second * (window_size_ms / 1000))

-- Check if request would exceed window limit
if current_requests + tokens_per_request > window_limit then
    -- Calculate retry after (time until next window)
    local next_window_start = window_start + window_size_ms
    local retry_after_ms = next_window_start - current_time

    -- Update window expiry for cleanup
    if current_requests > 0 then
        redis.call('EXPIRE', window_key, math.ceil(window_size_ms / 1000) + 60)
    end

    return {0, 0, retry_after_ms, current_requests}
end

-- Allow the request
local new_count = redis.call('INCRBY', window_key, tokens_per_request)

-- Set expiry for window cleanup
redis.call('EXPIRE', window_key, math.ceil(window_size_ms / 1000) + 60)

-- Calculate remaining capacity
local remaining = math.max(0, window_limit - new_count)

return {1, remaining, 0, new_count}