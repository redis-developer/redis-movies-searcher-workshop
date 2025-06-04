local result = redis.call('FT.AGGREGATE', 'imported_movies_index', '*',
  'LOAD', '2', '@title', '@id',
  'GROUPBY', '1', '@title',
  'REDUCE', 'COUNT', '0', 'AS', 'count',
  'REDUCE', 'TOLIST', '1', '@id', 'AS', 'ids',
  'FILTER', '@count > 1')

local deletion_count = 0
local index = 2  -- Start after the count
while index <= #result do
  local group = result[index]
  local title_idx, ids_idx = nil, nil

  -- Find title and ids indexes
  for i = 1, #group, 2 do
    if group[i] == 'title' then
      title_idx = i
    elseif group[i] == 'ids' then
      ids_idx = i
    end
  end

  if ids_idx then
    local ids_list = group[ids_idx + 1]
    -- Skip the first ID, delete the rest
    for i = 2, #ids_list do
      local key_name = 'import:movie:' .. ids_list[i]
      redis.call('DEL', key_name)
      deletion_count = deletion_count + 1
    end
  end

  index = index + 1
end

return 'Deleted ' .. deletion_count .. ' duplicate movies'
