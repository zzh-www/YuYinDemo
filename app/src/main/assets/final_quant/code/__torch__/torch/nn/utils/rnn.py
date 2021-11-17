def pad_sequence(sequences: List[Tensor],
    batch_first: bool=False,
    padding_value: float=0.) -> Tensor:
  max_size = torch.size(sequences[0])
  trailing_dims = torch.slice(max_size, 1, 9223372036854775807, 1)
  _0 = annotate(List[int], [])
  for _1 in range(torch.len(sequences)):
    s = sequences[_1]
    _2 = torch.append(_0, torch.size(s, 0))
  max_len = ops.prim.max(_0)
  if batch_first:
    out_dims0 = torch.add([torch.len(sequences), max_len], trailing_dims)
    out_dims = out_dims0
  else:
    out_dims1 = torch.add([max_len, torch.len(sequences)], trailing_dims)
    out_dims = out_dims1
  out_tensor = torch.new_full(sequences[0], out_dims, padding_value, dtype=None, layout=None, device=None, pin_memory=None)
  _3 = [9223372036854775807, torch.len(sequences)]
  for i in range(ops.prim.min(_3)):
    tensor = sequences[i]
    length = torch.size(tensor, 0)
    if batch_first:
      _4 = torch.slice(torch.select(out_tensor, 0, i), 0, 0, length, 1)
      _5 = torch.copy_(_4, tensor, False)
    else:
      _6 = torch.slice(out_tensor, 0, 0, length, 1)
      _7 = torch.copy_(torch.select(_6, 1, i), tensor, False)
  return out_tensor
