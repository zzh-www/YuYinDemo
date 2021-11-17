class PositionwiseFeedForward(Module):
  __parameters__ = []
  __buffers__ = []
  training : bool
  w_1 : __torch__.torch.nn.quantized.dynamic.modules.linear.Linear
  activation : __torch__.wenet.transformer.swish.Swish
  dropout : __torch__.torch.nn.modules.dropout.Dropout
  w_2 : __torch__.torch.nn.quantized.dynamic.modules.linear.Linear
  def forward(self: __torch__.wenet.transformer.positionwise_feed_forward.___torch_mangle_18.PositionwiseFeedForward,
    xs: Tensor) -> Tensor:
    _0 = self.w_2
    _1 = self.dropout
    _2 = (self.activation).forward((self.w_1).forward(xs, ), )
    return (_0).forward((_1).forward(_2, ), )
