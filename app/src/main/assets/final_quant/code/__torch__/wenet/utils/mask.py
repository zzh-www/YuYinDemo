def make_pad_mask(lengths: Tensor) -> Tensor:
  batch_size = torch.size(lengths, 0)
  max_len = int(torch.item(torch.max(lengths)))
  seq_range = torch.arange(0, max_len, dtype=4, layout=None, device=ops.prim.device(lengths), pin_memory=None)
  seq_range_expand = torch.expand(torch.unsqueeze(seq_range, 0), [batch_size, max_len], implicit=False)
  seq_length_expand = torch.unsqueeze(lengths, -1)
  mask = torch.ge(seq_range_expand, seq_length_expand)
  return mask
def add_optional_chunk_mask(xs: Tensor,
    masks: Tensor,
    use_dynamic_chunk: bool,
    use_dynamic_left_chunk: bool,
    decoding_chunk_size: int,
    static_chunk_size: int,
    num_decoding_left_chunks: int) -> Tensor:
  _0 = __torch__.wenet.utils.mask.subsequent_chunk_mask
  if use_dynamic_chunk:
    max_len = torch.size(xs, 1)
    if torch.lt(decoding_chunk_size, 0):
      chunk_size, num_left_chunks = max_len, -1
    else:
      if torch.gt(decoding_chunk_size, 0):
        chunk_size0, num_left_chunks0 = decoding_chunk_size, num_decoding_left_chunks
      else:
        _1 = torch.randint(1, max_len, [1], dtype=None, layout=None, device=None, pin_memory=None)
        chunk_size1 = torch.item(_1)
        _2 = torch.gt(chunk_size1, torch.floordiv(max_len, 2))
        if _2:
          chunk_size2, num_left_chunks1 = max_len, -1
        else:
          _3 = torch.remainder(chunk_size1, 25)
          chunk_size3 = torch.add(_3, 1)
          if use_dynamic_left_chunk:
            max_left_chunks = torch.floordiv(torch.sub(max_len, 1), chunk_size3)
            _4 = torch.randint(0, int(max_left_chunks), [1], dtype=None, layout=None, device=None, pin_memory=None)
            num_left_chunks3 = torch.item(_4)
            num_left_chunks2 = int(num_left_chunks3)
          else:
            num_left_chunks2 = -1
          chunk_size2, num_left_chunks1 = chunk_size3, num_left_chunks2
        chunk_size0, num_left_chunks0 = chunk_size2, num_left_chunks1
      chunk_size, num_left_chunks = chunk_size0, num_left_chunks0
    _5 = torch.size(xs, 1)
    _6 = ops.prim.device(xs)
    chunk_masks0 = _0(_5, int(chunk_size), num_left_chunks, _6, )
    chunk_masks1 = torch.unsqueeze(chunk_masks0, 0)
    chunk_masks = torch.__and__(masks, chunk_masks1)
  else:
    if torch.gt(static_chunk_size, 0):
      chunk_masks3 = _0(torch.size(xs, 1), static_chunk_size, num_decoding_left_chunks, ops.prim.device(xs), )
      chunk_masks4 = torch.unsqueeze(chunk_masks3, 0)
      chunk_masks5 = torch.__and__(masks, chunk_masks4)
      chunk_masks2 = chunk_masks5
    else:
      chunk_masks2 = masks
    chunk_masks = chunk_masks2
  return chunk_masks
def subsequent_mask(size: int,
    device: Device=torch.device("cpu")) -> Tensor:
  ret = torch.ones([size, size], dtype=11, layout=None, device=device, pin_memory=None)
  return torch.tril(ret, 0, out=ret)
def subsequent_chunk_mask(size: int,
    chunk_size: int,
    num_left_chunks: int=-1,
    device: Device=torch.device("cpu")) -> Tensor:
  ret = torch.zeros([size, size], dtype=11, layout=None, device=device, pin_memory=None)
  for i in range(size):
    if torch.lt(num_left_chunks, 0):
      start = 0
    else:
      _7 = torch.sub(torch.floordiv(i, chunk_size), num_left_chunks)
      start0 = ops.prim.max(torch.mul(_7, chunk_size), 0)
      start = start0
    _8 = torch.add(torch.floordiv(i, chunk_size), 1)
    ending = ops.prim.min(torch.mul(_8, chunk_size), size)
    _9 = torch.slice(torch.select(ret, 0, i), 0, start, ending, 1)
    _10 = torch.tensor(True, dtype=ops.prim.dtype(_9), device=ops.prim.device(_9), requires_grad=False)
    _11 = torch.copy_(_9, _10, False)
  return ret
