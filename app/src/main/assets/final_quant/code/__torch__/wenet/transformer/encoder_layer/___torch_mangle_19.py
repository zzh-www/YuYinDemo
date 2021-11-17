class ConformerEncoderLayer(Module):
  __parameters__ = []
  __buffers__ = []
  training : bool
  ff_scale : float
  size : int
  normalize_before : bool
  concat_after : bool
  self_attn : __torch__.wenet.transformer.attention.___torch_mangle_17.RelPositionMultiHeadedAttention
  feed_forward : __torch__.wenet.transformer.positionwise_feed_forward.___torch_mangle_18.PositionwiseFeedForward
  feed_forward_macaron : __torch__.wenet.transformer.positionwise_feed_forward.___torch_mangle_18.PositionwiseFeedForward
  conv_module : __torch__.wenet.transformer.convolution.ConvolutionModule
  norm_ff : __torch__.torch.nn.modules.normalization.LayerNorm
  norm_mha : __torch__.torch.nn.modules.normalization.LayerNorm
  norm_ff_macaron : __torch__.torch.nn.modules.normalization.LayerNorm
  norm_conv : __torch__.torch.nn.modules.normalization.LayerNorm
  norm_final : __torch__.torch.nn.modules.normalization.LayerNorm
  dropout : __torch__.torch.nn.modules.dropout.Dropout
  concat_linear : __torch__.torch.nn.quantized.dynamic.modules.linear.Linear
  def forward(self: __torch__.wenet.transformer.encoder_layer.___torch_mangle_19.ConformerEncoderLayer,
    x: Tensor,
    mask: Tensor,
    pos_emb: Tensor,
    mask_pad: Optional[Tensor]=None,
    output_cache: Optional[Tensor]=None,
    cnn_cache: Optional[Tensor]=None) -> Tuple[Tensor, Tensor, Tensor]:
    if self.normalize_before:
      x1 = (self.norm_ff_macaron).forward(x, )
      x0 = x1
    else:
      x0 = x
    _0 = self.ff_scale
    _1 = self.dropout
    _2 = (self.feed_forward_macaron).forward(x0, )
    x2 = torch.add(x, torch.mul((_1).forward(_2, ), _0), alpha=1)
    _3 = torch.__not__(self.normalize_before)
    if _3:
      x4 = (self.norm_ff_macaron).forward(x2, )
      x3 = x4
    else:
      x3 = x2
    if self.normalize_before:
      x5 = (self.norm_mha).forward(x3, )
    else:
      x5 = x3
    if torch.__is__(output_cache, None):
      x_q, mask0, residual, output_cache0 = x5, mask, x3, output_cache
    else:
      output_cache1 = unchecked_cast(Tensor, output_cache)
      _4 = torch.eq(torch.size(output_cache1, 0), torch.size(x5, 0))
      if _4:
        pass
      else:
        ops.prim.RaiseException("Exception")
      _5 = torch.eq(torch.size(output_cache1, 2), self.size)
      if _5:
        pass
      else:
        ops.prim.RaiseException("Exception")
      _6 = torch.lt(torch.size(output_cache1, 1), torch.size(x5, 1))
      if _6:
        pass
      else:
        ops.prim.RaiseException("Exception")
      chunk = torch.sub(torch.size(x5, 1), torch.size(output_cache1, 1))
      _7 = torch.slice(x5, 0, 0, 9223372036854775807, 1)
      _8 = torch.slice(_7, 1, torch.neg(chunk), 9223372036854775807, 1)
      x_q0 = torch.slice(_8, 2, 0, 9223372036854775807, 1)
      _9 = torch.slice(x3, 0, 0, 9223372036854775807, 1)
      _10 = torch.slice(_9, 1, torch.neg(chunk), 9223372036854775807, 1)
      residual0 = torch.slice(_10, 2, 0, 9223372036854775807, 1)
      _11 = torch.slice(mask, 0, 0, 9223372036854775807, 1)
      _12 = torch.slice(_11, 1, torch.neg(chunk), 9223372036854775807, 1)
      mask1 = torch.slice(_12, 2, 0, 9223372036854775807, 1)
      x_q, mask0, residual, output_cache0 = x_q0, mask1, residual0, output_cache1
    x_att = (self.self_attn).forward(x_q, x5, x5, mask0, pos_emb, )
    if self.concat_after:
      x_concat = torch.cat([x5, x_att], -1)
      _13 = (self.concat_linear).forward(x_concat, )
      x6 = torch.add(residual, _13, alpha=1)
    else:
      x7 = torch.add(residual, (self.dropout).forward(x_att, ), alpha=1)
      x6 = x7
    _14 = torch.__not__(self.normalize_before)
    if _14:
      x8 = (self.norm_mha).forward(x6, )
    else:
      x8 = x6
    if self.normalize_before:
      x9 = (self.norm_conv).forward(x8, )
    else:
      x9 = x8
    _15 = (self.conv_module).forward(x9, mask_pad, cnn_cache, )
    x10, new_cnn_cache, = _15
    x11 = torch.add(x8, (self.dropout).forward(x10, ), alpha=1)
    _16 = torch.__not__(self.normalize_before)
    if _16:
      x12 = (self.norm_conv).forward(x11, )
    else:
      x12 = x11
    if self.normalize_before:
      x13 = (self.norm_ff).forward(x12, )
    else:
      x13 = x12
    _17 = self.ff_scale
    _18 = (self.dropout).forward((self.feed_forward).forward(x13, ), )
    x14 = torch.add(x12, torch.mul(_18, _17), alpha=1)
    _19 = torch.__not__(self.normalize_before)
    if _19:
      x15 = (self.norm_ff).forward(x14, )
    else:
      x15 = x14
    x16 = (self.norm_final).forward(x15, )
    _20 = torch.__isnot__(output_cache0, None)
    if _20:
      output_cache2 = unchecked_cast(Tensor, output_cache0)
      x18 = torch.cat([output_cache2, x16], 1)
      x17 = x18
    else:
      x17 = x16
    return (x17, mask0, new_cnn_cache)
