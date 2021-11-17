class TransformerDecoder(Module):
  __parameters__ = []
  __buffers__ = []
  training : bool
  normalize_before : bool
  use_output_layer : bool
  num_blocks : int
  embed : __torch__.torch.nn.modules.container.___torch_mangle_11.Sequential
  after_norm : __torch__.torch.nn.modules.normalization.LayerNorm
  output_layer : __torch__.torch.nn.quantized.dynamic.modules.linear.Linear
  decoders : __torch__.torch.nn.modules.container.___torch_mangle_25.ModuleList
  def forward(self: __torch__.wenet.transformer.decoder.___torch_mangle_26.TransformerDecoder,
    memory: Tensor,
    memory_mask: Tensor,
    ys_in_pad: Tensor,
    ys_in_lens: Tensor,
    r_ys_in_pad: Optional[Tensor]=None,
    reverse_weight: float=0.) -> Tuple[Tensor, Tensor, Tensor]:
    _0 = __torch__.wenet.utils.mask.make_pad_mask
    _1 = __torch__.wenet.utils.mask.subsequent_mask
    _2 = torch.unsqueeze(_0(ys_in_lens, ), 1)
    tgt_mask = torch.to(torch.bitwise_not(_2), ops.prim.device(ys_in_pad), None, False, False)
    _3 = _1(torch.size(tgt_mask, -1), ops.prim.device(tgt_mask), )
    m = torch.unsqueeze(_3, 0)
    tgt_mask0 = torch.__and__(tgt_mask, m)
    x, _4, = (self.embed).forward(ys_in_pad, )
    _5 = self.decoders
    _6 = getattr(_5, "0")
    _7 = getattr(_5, "1")
    _8 = getattr(_5, "2")
    _9 = getattr(_5, "3")
    _10 = getattr(_5, "4")
    _11 = getattr(_5, "5")
    _12 = (_6).forward(x, tgt_mask0, memory, memory_mask, None, )
    x0, tgt_mask1, memory0, memory_mask0, = _12
    _13 = (_7).forward(x0, tgt_mask1, memory0, memory_mask0, None, )
    x1, tgt_mask2, memory1, memory_mask1, = _13
    _14 = (_8).forward(x1, tgt_mask2, memory1, memory_mask1, None, )
    x2, tgt_mask3, memory2, memory_mask2, = _14
    _15 = (_9).forward(x2, tgt_mask3, memory2, memory_mask2, None, )
    x3, tgt_mask4, memory3, memory_mask3, = _15
    _16 = (_10).forward(x3, tgt_mask4, memory3, memory_mask3, None, )
    x4, tgt_mask5, memory4, memory_mask4, = _16
    _17 = (_11).forward(x4, tgt_mask5, memory4, memory_mask4, None, )
    x5, tgt_mask6, memory5, memory_mask5, = _17
    if self.normalize_before:
      x6 = (self.after_norm).forward(x5, )
    else:
      x6 = x5
    if self.use_output_layer:
      x7 = (self.output_layer).forward(x6, )
    else:
      x7 = x6
    olens = torch.sum(tgt_mask6, [1], False, dtype=None)
    _18 = torch.tensor(0., dtype=None, device=None, requires_grad=False)
    return (x7, _18, olens)
