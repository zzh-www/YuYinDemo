class Dropout(Module):
  __parameters__ = []
  __buffers__ = []
  training : bool
  inplace : Final[bool] = False
  p : Final[float] = 0.
  def forward(self: __torch__.torch.nn.modules.dropout.___torch_mangle_3.Dropout,
    input: Tensor) -> Tensor:
    _0 = __torch__.torch.nn.functional.dropout
    _1 = _0(input, 0., self.training, False, )
    return _1
