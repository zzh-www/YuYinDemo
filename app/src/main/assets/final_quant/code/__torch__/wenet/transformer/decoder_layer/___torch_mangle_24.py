class DecoderLayer(Module):
  __parameters__ = []
  __buffers__ = []
  training : bool
  size : int
  normalize_before : bool
  concat_after : bool
  self_attn : __torch__.wenet.transformer.attention.___torch_mangle_22.MultiHeadedAttention
  src_attn : __torch__.wenet.transformer.attention.___torch_mangle_22.MultiHeadedAttention
  feed_forward : __torch__.wenet.transformer.positionwise_feed_forward.___torch_mangle_23.PositionwiseFeedForward
  norm1 : __torch__.torch.nn.modules.normalization.LayerNorm
  norm2 : __torch__.torch.nn.modules.normalization.LayerNorm
  norm3 : __torch__.torch.nn.modules.normalization.LayerNorm
  dropout : __torch__.torch.nn.modules.dropout.Dropout
  concat_linear1 : __torch__.torch.nn.quantized.dynamic.modules.linear.Linear
  concat_linear2 : __torch__.torch.nn.quantized.dynamic.modules.linear.Linear
  def forward(self: __torch__.wenet.transformer.decoder_layer.___torch_mangle_24.DecoderLayer,
    tgt: Tensor,
    tgt_mask: Tensor,
    memory: Tensor,
    memory_mask: Tensor,
    cache: Optional[Tensor]=None) -> Tuple[Tensor, Tensor, Tensor, Tensor]:
    if self.normalize_before:
      tgt0 = (self.norm1).forward(tgt, )
    else:
      tgt0 = tgt
    if torch.__is__(cache, None):
      tgt_q, tgt_q_mask, residual, cache0 = tgt0, tgt_mask, tgt, cache
    else:
      cache1 = unchecked_cast(Tensor, cache)
      _0 = torch.size(cache1)
      _1 = (torch.size(tgt0))[0]
      _2 = torch.sub((torch.size(tgt0))[1], 1)
      if torch.eq(_0, [_1, _2, self.size]):
        pass
      else:
        ops.prim.RaiseException("Exception")
      _3 = torch.slice(tgt0, 0, 0, 9223372036854775807, 1)
      _4 = torch.slice(_3, 1, -1, 9223372036854775807, 1)
      tgt_q0 = torch.slice(_4, 2, 0, 9223372036854775807, 1)
      _5 = torch.slice(tgt, 0, 0, 9223372036854775807, 1)
      _6 = torch.slice(_5, 1, -1, 9223372036854775807, 1)
      residual0 = torch.slice(_6, 2, 0, 9223372036854775807, 1)
      _7 = torch.slice(tgt_mask, 0, 0, 9223372036854775807, 1)
      _8 = torch.slice(_7, 1, -1, 9223372036854775807, 1)
      tgt_q_mask0 = torch.slice(_8, 2, 0, 9223372036854775807, 1)
      tgt_q, tgt_q_mask, residual, cache0 = tgt_q0, tgt_q_mask0, residual0, cache1
    if self.concat_after:
      _9 = (self.self_attn).forward(tgt_q, tgt0, tgt0, tgt_q_mask, CONSTANTS.c0, )
      tgt_concat = torch.cat([tgt_q, _9], -1)
      _10 = (self.concat_linear1).forward(tgt_concat, )
      x = torch.add(residual, _10, alpha=1)
    else:
      _11 = self.dropout
      _12 = (self.self_attn).forward(tgt_q, tgt0, tgt0, tgt_q_mask, CONSTANTS.c0, )
      x0 = torch.add(residual, (_11).forward(_12, ), alpha=1)
      x = x0
    _13 = torch.__not__(self.normalize_before)
    if _13:
      x1 = (self.norm1).forward(x, )
    else:
      x1 = x
    if self.normalize_before:
      x2 = (self.norm2).forward(x1, )
    else:
      x2 = x1
    if self.concat_after:
      _14 = (self.src_attn).forward(x2, memory, memory, memory_mask, CONSTANTS.c0, )
      x_concat = torch.cat([x2, _14], -1)
      _15 = (self.concat_linear2).forward(x_concat, )
      x3 = torch.add(x1, _15, alpha=1)
    else:
      _16 = self.dropout
      _17 = (self.src_attn).forward(x2, memory, memory, memory_mask, CONSTANTS.c0, )
      x4 = torch.add(x1, (_16).forward(_17, ), alpha=1)
      x3 = x4
    _18 = torch.__not__(self.normalize_before)
    if _18:
      x5 = (self.norm2).forward(x3, )
    else:
      x5 = x3
    if self.normalize_before:
      x6 = (self.norm3).forward(x5, )
    else:
      x6 = x5
    _19 = (self.dropout).forward((self.feed_forward).forward(x6, ), )
    x7 = torch.add(x5, _19, alpha=1)
    _20 = torch.__not__(self.normalize_before)
    if _20:
      x8 = (self.norm3).forward(x7, )
    else:
      x8 = x7
    if torch.__isnot__(cache0, None):
      cache2 = unchecked_cast(Tensor, cache0)
      x9 = torch.cat([cache2, x8], 1)
    else:
      x9 = x8
    return (x9, tgt_mask, memory, memory_mask)
